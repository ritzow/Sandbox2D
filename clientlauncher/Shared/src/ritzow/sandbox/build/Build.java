package ritzow.sandbox.build;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.spi.ToolProvider;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import static java.util.Map.entry;

public class Build {
	private static final String
		OS = "windows",
		ARCH = "x64",
		RELEASE_VERSION = Integer.toString(Runtime.version().feature());

	private static final Path SHARED_DIR = Path.of("..\\shared");
	private static final Path CLIENT_DIR = Path.of("..\\client");
	private static final Path OUTPUT_DIR = Path.of("Windows\\x64\\Release\\Output");
	private static final Path INCLUDE_DIR = Path.of("Windows\\include");

	private static final Charset SRC_CHARSET = StandardCharsets.UTF_8;

	public static void main(String... args) throws IOException, InterruptedException {
		System.out.println("Running Build.java.");
		if(args.length > 0) {
			switch(args[0].toLowerCase()) {
				case "launcher" -> msbuild();
				case "zip" -> packageOutput(Path.of(args[1]));
			}
		} else {
			buildAll();
		}
	}

	private static void buildAll() throws IOException, InterruptedException {
		if(Files.exists(OUTPUT_DIR)) {
			System.out.println("Deleting output directory.");
			traverse(OUTPUT_DIR, Files::delete, Files::delete);
		}

		Files.createDirectory(OUTPUT_DIR);
		Path TEMPORARY_OUT = createTempDir(OUTPUT_DIR);
		Path SHARED_SRC = SHARED_DIR.resolve("src");
		Path SHARED_OUT = TEMPORARY_OUT.resolve("shared");
		System.out.println("Searching for shared code.");
		var sharedFiles = getSourceFiles(SHARED_SRC);
		System.out.println("Compiling shared code.");
		if(compile(SHARED_OUT, SHARED_SRC, sharedFiles, List.of())) {
			Path CLIENT_LIBS = CLIENT_DIR.resolve("libraries");
			Path LWJGL_DIR = CLIENT_LIBS.resolve("lwjgl");
			System.out.println("Shared code compiled.");
			Collection<Path> modules = new ArrayList<Path>(6);
			modules.add(LWJGL_DIR.resolve("lwjgl.jar"));
			modules.add(LWJGL_DIR.resolve("lwjgl-glfw.jar"));
			modules.add(LWJGL_DIR.resolve("lwjgl-opengl.jar"));
			modules.add(LWJGL_DIR.resolve("lwjgl-openal.jar"));
			modules.add(SHARED_OUT);
			Path CLIENT_SRC = CLIENT_DIR.resolve("src");
			Path CLIENT_OUT = TEMPORARY_OUT.resolve("client");
			System.out.println("Searching for client code.");
			var clientFiles = getSourceFiles(CLIENT_SRC);
			System.out.println("Compiling client code.");
			if(compile(CLIENT_OUT, CLIENT_SRC, clientFiles, modules)) {
				System.out.print("Client code compiled.\nRunning jlink... ");
				modules.add(CLIENT_OUT);
				Path JVM_DIR = OUTPUT_DIR.resolve("jvm");
				int result = jlink(JVM_DIR, modules, List.of(
					"ritzow.sandbox.client",
					"jdk.management.agent",
					"jdk.management.jfr"
				));

				if(result == 0) {
					postJlink(CLIENT_DIR, JVM_DIR, LWJGL_DIR,
						CLIENT_LIBS.resolve("json"), OUTPUT_DIR, INCLUDE_DIR);
				} else {
					System.out.println("jlink failed with error " + result + ".");
				}
			} else {
				System.out.println("Client compilation failed.");
			}
		} else {
			System.out.println("Shared compilation failed.");
		}
	}

	private static void postJlink(Path CLIENT_DIR, Path JVM_DIR, Path LWJGL_DIR,
			Path JSON_DIR, Path OUTPUT_DIR, Path INCLUDE_DIR) throws IOException, InterruptedException {
		System.out.println("done.\nMoving header files and deleting unecessary files.");
		traverse(INCLUDE_DIR, Files::delete, Files::delete);

		//create include directory
		Files.createDirectory(INCLUDE_DIR);

		//move header files to Windows directory
		traverse(JVM_DIR.resolve("include"), file ->
			Files.move(file, INCLUDE_DIR.resolve(file.getFileName())), Files::delete);

		//delete unnecessary java.base files
		Files.delete(JVM_DIR.resolve("bin").resolve("java.exe"));
		Files.delete(JVM_DIR.resolve("bin").resolve("javaw.exe"));
		Files.delete(JVM_DIR.resolve("bin").resolve("keytool.exe"));
		Files.delete(JVM_DIR.resolve("lib").resolve("jvm.lib"));

		System.out.print("Copying game files and natives... ");

		//copy resources while preserving file system structure
		Path RESOURCES_SRC_DIR = CLIENT_DIR.resolve("resources");
		Path RESOURCES_OUT_DIR = OUTPUT_DIR.resolve("resources");
		Files.walk(RESOURCES_SRC_DIR).forEach(path -> {
			try {
				Files.copy(path, RESOURCES_OUT_DIR.resolve(RESOURCES_SRC_DIR.relativize(path)));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		//copy options.txt and .dll files into output directory
		Files.copy(CLIENT_DIR.resolve("options.txt"), OUTPUT_DIR.resolve("options.txt"));
		List<Entry<String, String>> natives = List.of(
			entry("lwjgl-natives-windows.jar", "org/lwjgl"),
			entry("lwjgl-glfw-natives-windows.jar", "org/lwjgl/glfw"),
			entry("lwjgl-openal-natives-windows.jar", "org/lwjgl/openal"),
			entry("lwjgl-opengl-natives-windows.jar", "org/lwjgl/opengl")
		);

		for(var entry : natives) {
			try(FileSystem jar = FileSystems.newFileSystem(LWJGL_DIR.resolve(entry.getKey()));
				var dllFiles = Files.newDirectoryStream(
						jar.getPath(OS + "/" + ARCH, entry.getValue()), "*.dll")) {
				for(Path dll : dllFiles) {
					Files.copy(dll, OUTPUT_DIR.resolve(dll.getFileName().toString()));
				}
			}
		}

		//copy legal files to a single output location
		System.out.print("done.\nCopying legal files... ");
		Path LEGAL_DIR = OUTPUT_DIR.resolve("legal");
		Files.move(JVM_DIR.resolve("legal"), LEGAL_DIR);
		Files.copy(LWJGL_DIR.resolve("LICENSE"), LEGAL_DIR.resolve("lwjgl_license.txt"));
		Files.copy(JSON_DIR.resolve("LICENSE"), LEGAL_DIR.resolve("json_license.txt"));
		try(var stream = Files.newDirectoryStream(LWJGL_DIR, "*license.txt")) {
			for(Path file : stream) {
				Files.copy(file, LEGAL_DIR.resolve(file.getFileName()));
			}
		}

		//run launcher executable build script
		System.out.println("done.\nBuilding launcher executable.");
		int result = msbuild();
		System.out.println(result == 0 ? "Launcher built." : "Launcher build failed with code " + result);
	}

	private static int msbuild() throws IOException, InterruptedException {
		return new ProcessBuilder(
			"msbuild",
			"-nologo",
			"-verbosity:minimal",
			"Sandbox2DClientLauncher.vcxproj",
			"-p:Platform=" + ARCH + ";Configuration=Release"
		).inheritIO().start().waitFor();
	}

	private static void packageOutput(Path outFile) throws IOException {
		Instant compileTime = Instant.now();
		System.out.println("Zipping program to " + outFile);
		try(var zip  = new ZipOutputStream(Files.newOutputStream(outFile))) {
			zip.setComment("Sandbox2D Game Client Binaries");
			zip.setLevel(Deflater.BEST_COMPRESSION);
			zip.setMethod(ZipOutputStream.DEFLATED);
			traverse(OUTPUT_DIR, file -> {
				ZipEntry entry = new ZipEntry(OUTPUT_DIR.relativize(file).toString().replace('\\', '/'));
				entry.setCreationTime(FileTime.from(compileTime));
				entry.setLastModifiedTime(FileTime.from(compileTime));
				zip.putNextEntry(entry);
				zip.write(Files.readAllBytes(file));
				zip.closeEntry();
			}, path -> {});
		}
		NumberFormat format = NumberFormat.getIntegerInstance();
		format.setGroupingUsed(true);
		System.out.println("Zipped to " + format.format(Files.size(outFile)) + " bytes.");
	}

	private static Iterable<? extends JavaFileObject> getSourceFiles(Path src) throws IOException {
		PathMatcher match = src.getFileSystem().getPathMatcher("glob:*.java");
		Path diagDir = src.getParent();
		List<JavaFileObject> list = new ArrayList<JavaFileObject>();
		Files.find(src, Integer.MAX_VALUE, (path, attr) ->
				!attr.isDirectory() && match.matches(path.getFileName()))
			.peek(file -> System.out.println("FOUND " + diagDir.relativize(file)))
			.map(PathSourceFile::new)
			.forEach(list::add);
		return list;
	}

	private static boolean compile(Path output, Path diag, Iterable<? extends JavaFileObject> sources,
			Iterable<Path> modules) {
		return javax.tools.ToolProvider.getSystemJavaCompiler().getTask(
			null,
			null,
			createDiagnostic(diag),
			List.of(
			"-g:none",
			"--enable-preview",
			"--release", RELEASE_VERSION,
			"--module-path", join(modules, ';'),
			"-d", output.toString()
			),
			null,
			sources
		).call();
	}

	private static int jlink(Path output, Iterable<Path> modules, Iterable<String> includeModules) {
		return ToolProvider.findFirst("jlink").get().run(System.out, System.err,
			"--compress", "2",
			//"--strip-debug",
			"--no-man-pages",
			"--endian", "little",
			"--module-path", join(modules, ';'),
			"--add-modules", join(includeModules, ','),
			"--output", output.toString()
		);
	}

	private static DiagnosticListener<JavaFileObject> createDiagnostic(Path baseDir) {
		return diagnostic -> {
			StringBuilder msg = new StringBuilder(diagnostic.getKind().name()).append(" ");
			if(diagnostic.getSource() == null) {
				msg.append(diagnostic.getMessage(Locale.getDefault()));
			} else {
				msg.append(baseDir.relativize(Path.of(diagnostic.getSource().getName())));
				if(diagnostic.getLineNumber() != Diagnostic.NOPOS) {
					msg.append(":").append(diagnostic.getLineNumber());
				}
				msg.append("\n\t").append(diagnostic.getMessage(Locale.getDefault()));
			}
			System.out.println(msg);
		};
	}

	private static Path createTempDir(Path output) throws IOException {
		Path temp = Files.createTempDirectory(output, "classes_");
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				traverse(temp, Files::delete, Files::delete);
			} catch (IOException e) {
				throw new RuntimeException("Error while deleting temp directory.", e);
			}
		}));
		return temp;
	}

	private static <T> String join(Iterable<T> elements, char joinChar) {
		StringJoiner joiner = new StringJoiner(Character.toString(joinChar));
		for(T t : elements) {
			joiner.add(t.toString());
		}
		return joiner.toString();
	}

	private interface PathConsumer {
		void accept(Path path) throws IOException;
	}

	private static void traverse(Path directory,
			PathConsumer fileAction, PathConsumer dirAction) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				fileAction.accept(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		        if (exc != null) throw exc;
				dirAction.accept(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static final class PathSourceFile implements JavaFileObject {
		private final Path path;

		private PathSourceFile(Path path) {
			this.path = path;
		}

		@Override
		public URI toUri() {
			return path.toUri().normalize();
		}

		@Override
		public String getName() {
			return path.toString();
		}

		@Override
		public Kind getKind() {
			return Kind.SOURCE;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			return Files.readString(path, SRC_CHARSET);
		}

		@Override
		public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
			return Files.newBufferedReader(path, SRC_CHARSET);
		}

		@Override
		public InputStream openInputStream() throws IOException {
			return Files.newInputStream(path);
		}

		@Override
		public OutputStream openOutputStream() {
			throw new UnsupportedOperationException("writes to source files not allowed");
		}

		@Override
		public Writer openWriter() {
			throw new UnsupportedOperationException("writes to source files not allowed");
		}

		@Override
		public long getLastModified() {
			try {
				return Files.getLastModifiedTime(path).toMillis();
			} catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
		}

		@Override
		public boolean delete() {
			throw new UnsupportedOperationException("source file modifications not allowed");
		}

		@Override
		public boolean isNameCompatible(String simpleName, Kind kind) {
			return kind == Kind.SOURCE && path.getFileName().toString().equals(simpleName + kind.extension);
		}

		@Override
		public NestingKind getNestingKind() {
			return NestingKind.TOP_LEVEL;
		}

		@Override
		public Modifier getAccessLevel() {
			return null;
		}
	}
}