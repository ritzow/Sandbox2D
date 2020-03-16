package ritzow.sandbox.build;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.*;
import javax.tools.JavaFileObject.Kind;

import static java.util.Map.entry;

public class Build {

	private static final boolean DEBUG = false;

	private static final String
		OS = "windows",
		ARCH = "x64",
		RELEASE_VERSION = Integer.toString(Runtime.version().feature()),
		AUTHOR = "Solomon Ritzow";

	private static final Path
		LAUNCHER_DIR =  Path.of(System.getProperty("user.dir")),
		SHARED_DIR =    LAUNCHER_DIR.resolveSibling("shared"),
		SHARED_SRC = 	SHARED_DIR.resolve("src"),
		CLIENT_DIR =	LAUNCHER_DIR.resolveSibling("client"),
		CLIENT_SRC = 	CLIENT_DIR.resolve("src"),
		CLIENT_LIBS = 	CLIENT_DIR.resolve("libraries"),
		RES_SRC_DIR = 	CLIENT_DIR.resolve("resources"),
		LWJGL_DIR = 	CLIENT_LIBS.resolve("lwjgl"),
		JSON_DIR =		CLIENT_LIBS.resolve("json"),
		WINDOWS_DIR = 	LAUNCHER_DIR.resolve("Windows"),
		RELEASE_DIR = 	WINDOWS_DIR.resolve(ARCH).resolve("Release"),
		BUILD_DIR = 	RELEASE_DIR.resolve("Build"),
		CLIENT_LIB = 	BUILD_DIR.resolve("client.jar"),
		SHARED_LIB = 	BUILD_DIR.resolve("shared.jar"),
		OUTPUT_DIR = 	RELEASE_DIR.resolve("Output"),
		RES_OUT_DIR = 	OUTPUT_DIR.resolve("resources"),
		JVM_DIR =		OUTPUT_DIR.resolve("jvm"),
		JVM_INCLUDES =	JVM_DIR.resolve("include"),
		INCLUDE_DIR = 	WINDOWS_DIR.resolve("include"),
		MSBUILD_FILE = 	WINDOWS_DIR.resolve("Sandbox2DLauncherWindows.vcxproj");

	private static final Charset SRC_CHARSET = StandardCharsets.UTF_8;

	public static void main(String... args) throws IOException, InterruptedException {
		System.out.println("Running Build.java.");
		if(args.length > 0) {
			switch(args[0].toLowerCase()) {
				case "executable" -> System.out.println(msbuild() == 0 ? "Launcher built."
					: "Launcher build failed.");
				case "zip" -> packageOutput(args.length > 1 ? Path.of(args[1]) : Path.of("out.zip"));
				default -> System.out.println("Uknown arguments.");
			}
		} else {
			buildAll();
		}
	}

	private static void buildAll() throws IOException, InterruptedException {
		if(Files.exists(OUTPUT_DIR)) {
			System.out.println("Deleting output directory.");
			forEachFileAndDir(OUTPUT_DIR, Files::delete);
			Files.delete(OUTPUT_DIR);
		}

		Files.createDirectory(OUTPUT_DIR);
		System.out.println("Searching for shared code.");
		var sharedFiles = getSourceFiles(SHARED_SRC);
		System.out.println("Compiling shared code.");
		if(compile(SHARED_LIB, SHARED_SRC, sharedFiles, List.of())) {
			System.out.println("Shared code compiled.");
			Collection<Path> modules = new ArrayList<Path>(6);
			modules.add(LWJGL_DIR.resolve("lwjgl.jar"));
			modules.add(LWJGL_DIR.resolve("lwjgl-glfw.jar"));
			modules.add(LWJGL_DIR.resolve("lwjgl-opengl.jar"));
			modules.add(LWJGL_DIR.resolve("lwjgl-openal.jar"));
			modules.add(SHARED_LIB);
			System.out.println("Searching for client code.");
			var clientFiles = getSourceFiles(CLIENT_SRC);
			System.out.println("Compiling client code.");
			if(compile(CLIENT_LIB, CLIENT_SRC, clientFiles, modules)) {
				System.out.print("Client code compiled.\nRunning jlink... ");
				modules.add(CLIENT_LIB);
				Collection<String> moduleNames = new ArrayList<>();
				moduleNames.add("ritzow.sandbox.client");
				if(DEBUG) {
					moduleNames.add("jdk.management.agent");
					moduleNames.add("jdk.management.jfr");
				}
				int result = jlink(modules, moduleNames);
				if(result == 0) {
					postJlink();
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

	private static void postJlink()
		throws IOException, InterruptedException {
		System.out.println("done.\nMoving header files and deleting unecessary files.");
		if(Files.exists(INCLUDE_DIR)) {
			forEachFileAndDir(INCLUDE_DIR, Files::delete);
		}

		//create include directory if it doesn't exist
		if(!Files.isDirectory(INCLUDE_DIR))
			Files.createDirectory(INCLUDE_DIR);

		//move header files to Windows directory
		forEachFileAndDir(JVM_INCLUDES, path -> {
			if(Files.isDirectory(path)) {
				Files.delete(path);
			} else if(Files.isRegularFile(path)) {
				Files.move(path, INCLUDE_DIR.resolve(path.getFileName()));
			}
		});
		Files.delete(JVM_INCLUDES);

		//delete unnecessary java.base files
		Files.delete(JVM_DIR.resolve("bin").resolve("java.exe"));
		Files.delete(JVM_DIR.resolve("bin").resolve("javaw.exe"));
		Files.delete(JVM_DIR.resolve("bin").resolve("keytool.exe"));
		Files.delete(JVM_DIR.resolve("lib").resolve("jvm.lib"));

		System.out.print("Copying game files and natives... ");

		//copy resources while preserving file system structure
		Files.walk(RES_SRC_DIR).forEach(path -> {
			try {
				Files.copy(path, RES_OUT_DIR.resolve(RES_SRC_DIR.relativize(path)));
			} catch(IOException e) {
				throw new RuntimeException("Failed to copy client resources", e);
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
			MSBUILD_FILE.toString(),
			"-p:Configuration=Release;Platform=" + ARCH
		).inheritIO().start().waitFor();
	}

	//TODO can use FileSystem API to do this as well
	private static void packageOutput(Path outFile) throws IOException {
		System.out.println("Zipping program to " + outFile);
		try(var zip = new ZipOutputStream(Files.newOutputStream(outFile))) {
			zip.setComment("Sandbox2D Game Client Binaries");
			zip.setLevel(Deflater.BEST_COMPRESSION);
			zip.setMethod(ZipOutputStream.DEFLATED);
			Instant compileTime = Instant.now();
			forEachFile(OUTPUT_DIR, Files::isReadable, file -> {
				try {
					ZipEntry entry = new ZipEntry(OUTPUT_DIR.relativize(file).toString().replace('\\', '/'));
					entry.setCreationTime(FileTime.from(compileTime));
					entry.setLastModifiedTime(FileTime.from(compileTime));
					zip.putNextEntry(entry);
					zip.write(Files.readAllBytes(file));
					zip.closeEntry();
				} catch(IOException e) {
					throw new RuntimeException("Failed to zip", e);
				}
			});
		}
		System.out.println("Zipped to " + NumberFormat.getInstance().format(Files.size(outFile)) + " bytes.");
	}

	private static Iterable<JavaFileObject> getSourceFiles(Path src) throws IOException {
		Collection<JavaFileObject> files = new ArrayList<>();
		PathMatcher matcher = file -> file.getFileName().toString().endsWith(".java");
		forEachFile(src, matcher, file -> files.add(new SourceFile(file)));
		return files;
	}

	private static boolean compile(Path outJar, Path diag,
		Iterable<JavaFileObject> sources, Collection<Path> modules) throws IOException {
		JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
		var diagnostics = createDiagnostic(diag);
		try(var fileManager = new JarOutFileManager(javac, diagnostics, outJar, modules)) {
			return javac.getTask(
				null, //writer
				fileManager, //file manager
				diagnostics, //diagnostic listener
				List.of( //options
					"-g:none",
					"--enable-preview",
					"--release", RELEASE_VERSION
				),
				null, //annotation processed class names
				sources //compilation units (source files)
			).call();
		}
	}

	private static int jlink(Iterable<Path> modules, Iterable<String> includeModules) {
		return java.util.spi.ToolProvider.findFirst("jlink").orElseThrow().run(System.out, System.err,
			"--compress", "2",
			//"--strip-debug",
			"--no-man-pages",
			"--endian", "little",
			"--module-path", join(modules, ';'),
			"--add-modules", join(includeModules, ','),
			"--output", JVM_DIR.toString()
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

	private static void forEachFileAndDir(Path dir, PathConsumer action) throws IOException {
		try(var paths = Files.newDirectoryStream(dir)) {
			for(Path path : paths) {
				if(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
					action.accept(path);
				} else if(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
					forEachFileAndDir(path, action);
					action.accept(path);
				}
			}
		}
	}

	private static void forEachFile(Path dir, PathMatcher matcher, PathConsumer action) throws IOException {
		try(var paths = Files.newDirectoryStream(dir,
			path -> Files.isDirectory(path) || matcher.matches(path))) {
			for(Path path : paths) {
				if(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
					action.accept(path);
				} else if(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
					forEachFile(path, matcher, action);
				}
			}
		}
	}

	//TODO write directly to a jar file to use up less space
	private static class JarOutFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
		private final Path outJar;
		private final ByteArrayOutputStream buffer;
		private final JarOutputStream outFile;

		public JarOutFileManager(JavaCompiler compiler, DiagnosticListener<JavaFileObject> listener,
			Path outJar, Collection<Path> modules) throws IOException {
			super(compiler.getStandardFileManager(listener, Locale.getDefault(), SRC_CHARSET));
			fileManager.setLocationFromPaths(StandardLocation.MODULE_PATH, modules);
			this.outJar = outJar;
			Manifest manifest = new Manifest();
			Attributes attribs = manifest.getMainAttributes();
			attribs.put(Name.MANIFEST_VERSION, "1.0");
			attribs.put(Name.SPECIFICATION_TITLE, "Sandbox2D");
			attribs.put(Name.SPECIFICATION_VENDOR, AUTHOR);
			outFile = new JarOutputStream(buffer = new ByteArrayOutputStream(50_000), manifest);
			outFile.setComment("Sandbox2D binaries, by " + AUTHOR);
			outFile.setLevel(Deflater.BEST_COMPRESSION);
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location,
		    String className, Kind kind, FileObject sibling) {
			if(location instanceof StandardLocation type) {
				return switch(type) {
					default -> throw new UnsupportedOperationException(location + " " + className);
					case CLASS_OUTPUT -> {
						System.out.println("OUTPUT FROM " +
							Path.of(sibling.getName()).getFileName() + " TO " + className);
						yield new JavaJarEntry(className.replace('.', '/') + kind.extension, outFile);
					}
				};
			} else {
				throw new UnsupportedOperationException(className);
			}
		}

		@Override
		public boolean hasLocation(Location location) {
			return location == StandardLocation.MODULE_PATH;
		}

		@Override
		public Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException {
			return location instanceof StandardLocation type ? switch(type) {
				case MODULE_PATH -> super.listLocationsForModules(location);
				default -> List.of();
			} : List.of();
		}

		@Override
		public void close() throws IOException {
			outFile.close();
			try(var out = Files.newOutputStream(outJar)) {
				buffer.writeTo(out);
			}
			super.close();
		}
	}

	private static class SourceFile implements JavaFileObject {
		private final Path path;

		private SourceFile(Path path) {
			this.path = path;
		}

		@Override
		public URI toUri() {
			return path.normalize().toUri();
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
			throw new UnsupportedOperationException();
		}

		@Override
		public Writer openWriter() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getLastModified() {
			try {
				return Files.getLastModifiedTime(path).toMillis();
			} catch(IOException e) {
				e.printStackTrace();
				return 0;
			}
		}

		@Override
		public boolean delete() {
			throw new UnsupportedOperationException();
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

	private static class JavaJarEntry extends JarEntry implements JavaFileObject {
		private final JarOutputStream output;

		public JavaJarEntry(String name, JarOutputStream out) {
			super(name);
			this.output = out;
		}

		@Override
		public Kind getKind() {
			return Kind.CLASS;
		}

		@Override
		public boolean isNameCompatible(String simpleName, Kind kind) {
			throw new UnsupportedOperationException();
		}

		@Override
		public NestingKind getNestingKind() {
			return null;
		}

		@Override
		public Modifier getAccessLevel() {
			return null;
		}

		@Override
		public URI toUri() {
			throw new UnsupportedOperationException();
		}

		@Override
		public InputStream openInputStream() {
			throw new UnsupportedOperationException();
		}

		@Override
		public OutputStream openOutputStream() throws IOException {
			output.putNextEntry(this);
			return new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					output.write(b);
				}

				@Override
				public void write(byte[] b) throws IOException {
					output.write(b);
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					output.write(b, off, len);
				}

				@Override
				public void flush() throws IOException {
					output.flush();
				}

				@Override
				public void close() throws IOException {
					flush();
					output.closeEntry();
				}
			};
		}

		@Override
		public Reader openReader(boolean ignoreEncodingErrors) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Writer openWriter() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getLastModified() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean delete() {
			throw new UnsupportedOperationException();
		}
	}
}