package ritzow.sandbox.build;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

public class Build {
	private static final String 
		OS = "windows", 
		ARCH = "x64", 
		RELEASE_VERSION = Integer.toString(Runtime.version().feature());
	
	private static final Charset SRC_CHARSET = StandardCharsets.UTF_8;
	
	public static void main(String... args) throws IOException, InterruptedException {
		System.out.println("Running Build.java.");
		Path SHARED_DIR = Path.of(args[0]);
		Path CLIENT_DIR = Path.of(args[1]);
		Path OUTPUT_DIR = Path.of(args[2]);
		Path INCLUDE_DIR = Path.of(args[3]);

		if(Files.exists(OUTPUT_DIR)) {
			System.out.println("Deleting output directory.");
			traverse(OUTPUT_DIR, Files::delete, Files::delete);
		}
		
		Files.createDirectory(OUTPUT_DIR); //TODO could cause exception? Windows bug
		Path temp = createTempDir(OUTPUT_DIR);
		Path SHARED_SRC = SHARED_DIR.resolve("src");
		Path SHARED_OUT = temp.resolve("shared");
		System.out.println("Searching for shared code.");
		var sharedFiles = getSourceFiles(SHARED_SRC);
		System.out.println("Compiling shared code.");
		if(compile(SHARED_OUT, SHARED_SRC, sharedFiles, List.of())) {
			Path CLIENT_LIBS = CLIENT_DIR.resolve("libraries");
			Path LWJGL_DIR = CLIENT_LIBS.resolve("lwjgl");
			System.out.println("Shared code compiled.");
			List<Path> modules = new ArrayList<Path>(6);
			modules.add(LWJGL_DIR.resolve("lwjgl.jar"));
			modules.add(LWJGL_DIR.resolve("lwjgl-glfw.jar"));
			modules.add(LWJGL_DIR.resolve("lwjgl-opengl.jar"));
			modules.add(LWJGL_DIR.resolve("lwjgl-openal.jar"));
			modules.add(SHARED_OUT);
			Path CLIENT_SRC = CLIENT_DIR.resolve("src");
			Path CLIENT_OUT = temp.resolve("client");
			System.out.println("Searching for client code.");
			var clientFiles = getSourceFiles(CLIENT_SRC);
			System.out.println("Compiling client code.");
			if(compile(CLIENT_OUT, CLIENT_SRC, clientFiles, modules)) {
				System.out.print("Client code compiled.\nRunning jlink... ");
				modules.add(CLIENT_OUT);
				Path JVM_DIR = OUTPUT_DIR.resolve("jvm");
				int result = jlink(JVM_DIR, modules, 
					"java.base",
					"ritzow.sandbox.client",
					"ritzow.sandbox.shared",
					"jdk.unsupported",
					"org.lwjgl",
					"org.lwjgl.glfw",
					"org.lwjgl.opengl",
					"org.lwjgl.openal"
				);
				
				if(result == 0) {
					postJlink(CLIENT_DIR, JVM_DIR, LWJGL_DIR, CLIENT_LIBS, OUTPUT_DIR, INCLUDE_DIR);
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
	
	private static void postJlink(Path CLIENT_DIR, Path JVM_DIR, 
			Path LWJGL_DIR, Path CLIENT_LIBS, Path OUTPUT_DIR, Path INCLUDE_DIR) 
					throws IOException, InterruptedException {
		System.out.println("done.\nMoving header files and deleting unecessary files.");
		traverse(INCLUDE_DIR, Files::delete, Files::delete);
		Files.createDirectories(INCLUDE_DIR);
		traverse(JVM_DIR.resolve("include"), file -> 
			Files.move(file, INCLUDE_DIR.resolve(file.getFileName())), Files::delete);
		Files.delete(JVM_DIR.resolve("bin").resolve("java.exe"));
		Files.delete(JVM_DIR.resolve("bin").resolve("javaw.exe"));
		Files.delete(JVM_DIR.resolve("bin").resolve("keytool.exe"));
		Files.delete(JVM_DIR.resolve("lib").resolve("jvm.lib"));
		
		System.out.print("Copying game files and natives... ");
		Path RESOURCES_SRC_DIR = CLIENT_DIR.resolve("resources");
		Path RESOURCES_DIR = OUTPUT_DIR.resolve("resources");
		Files.walk(RESOURCES_SRC_DIR).forEach(path -> {
			try {
				Files.copy(path, RESOURCES_DIR.resolve(RESOURCES_SRC_DIR.relativize(path)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		//copy options.txt and .dll files into output directory
		Files.copy(CLIENT_DIR.resolve("options.txt"), OUTPUT_DIR.resolve("options.txt"));
		extractNatives(LWJGL_DIR, OUTPUT_DIR, OS + "/" + ARCH, List.of(
			Map.entry("lwjgl-natives-windows.jar", "org/lwjgl"),
			Map.entry("lwjgl-glfw-natives-windows.jar", "org/lwjgl/glfw"),
			Map.entry("lwjgl-openal-natives-windows.jar", "org/lwjgl/openal"),
			Map.entry("lwjgl-opengl-natives-windows.jar", "org/lwjgl/opengl")
		));
		
		//copy legal files to a single output location
		System.out.print("done.\nCopying legal files... ");
		Path LEGAL_DIR = OUTPUT_DIR.resolve("legal");
		Files.move(JVM_DIR.resolve("legal"), LEGAL_DIR);
		for(Path file : Files.newDirectoryStream(LWJGL_DIR, "*license.txt")) {
			Files.copy(file, LEGAL_DIR.resolve(file.getFileName().toString()));
		}
		Files.copy(LWJGL_DIR.resolve("LICENSE"), LEGAL_DIR.resolve("lwjgl_license.txt"));
		Files.copy(CLIENT_LIBS.resolve("json").resolve("LICENSE"), LEGAL_DIR.resolve("json_license.txt"));
		
		//run launcher executable build script
		System.out.println("done.\nBuilding launcher executable.");
		msbuild();
	}
	
	private static void msbuild() throws IOException, InterruptedException {
		Process msbuild = new ProcessBuilder(
			"msbuild", 
			"-nologo",
			"-verbosity:minimal",
			"Sandbox2DClientLauncher.vcxproj",
			"-p:Platform=" + ARCH + ";Configuration=Release"
		).inheritIO().start();
		int status = msbuild.waitFor();
		System.out.println(status == 0 ? "Launcher built." : 
			"Launcher build failed with code " + status);
	}
	
	private static void extractNatives(Path libDir, Path outDir, 
			String startDir, List<Entry<String, String>> jarNames) throws IOException {
		for(var entry : jarNames) {
			try(FileSystem jar = FileSystems.newFileSystem(libDir.resolve(entry.getKey()))) {
				for(Path file : Files.newDirectoryStream(jar.getPath(startDir, entry.getValue()), "*.dll")) {
					Files.copy(file, outDir.resolve(file.getFileName().toString()));	
				}
			}
		}
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
	
	private static int jlink(Path output, Iterable<Path> modules, String... moduleNames) {
		return ToolProvider.findFirst("jlink").get().run(System.out, System.err, 
			"--compress", "2",
			"--no-man-pages",
			"--endian", "little",
			//"--strip-debug",
			"--module-path", join(modules, ';'),
			"--add-modules", String.join(",", moduleNames),
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
					msg.append(":" + diagnostic.getLineNumber());
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
	
	private static interface PathConsumer {
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
		public InputStream openInputStream() throws IOException {
			return Files.newInputStream(path);
		}

		@Override
		public OutputStream openOutputStream() throws IOException {
			throw new UnsupportedOperationException("writes to source files not allowed");
		}

		@Override
		public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
			return Files.newBufferedReader(path, SRC_CHARSET);
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			return Files.readString(path, SRC_CHARSET);
		}

		@Override
		public Writer openWriter() throws IOException {
			throw new UnsupportedOperationException("writes to source files not allowed");
		}

		@Override
		public long getLastModified() {
			try {
				return Files.getLastModifiedTime(path).toInstant().toEpochMilli();
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
		public Kind getKind() {
			return Kind.SOURCE;
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