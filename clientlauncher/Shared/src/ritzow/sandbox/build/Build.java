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
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.*;

import static java.util.Map.entry;

public class Build {

	private static final boolean DEBUG = false;

	private static final String
		OS = "windows",
		ARCH = "x64",
		RELEASE_VERSION = Integer.toString(Runtime.version().feature());

	private static final Path
		SHARED_DIR = Path.of("..\\shared"),
		CLIENT_DIR = Path.of("..\\client"),
		OUTPUT_DIR = Path.of("Windows\\x64\\Release\\Output"),
		INCLUDE_DIR = Path.of("Windows\\include"),
		LAUNCHER_PROJECT_FILE = Path.of("Sandbox2DClientLauncher.vcxproj");

	private static final Charset SRC_CHARSET = StandardCharsets.UTF_8;

	public static void main(String... args) throws IOException, InterruptedException {
		System.out.println("Running Build.java.");
		if(args.length > 0) {
			switch(args[0].toLowerCase()) {
				case "launcher" -> msbuild();
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
				Collection<String> moduleNames = new ArrayList<>();
				moduleNames.add("ritzow.sandbox.client");
				if(DEBUG) {
					moduleNames.add("jdk.management.agent");
					moduleNames.add("jdk.management.jfr");
				}
				Path JVM_DIR = OUTPUT_DIR.resolve("jvm");
				int result = jlink(JVM_DIR, modules, moduleNames);
				if(result == 0) {
					postJlink(JVM_DIR, LWJGL_DIR, CLIENT_LIBS.resolve("json"));
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

	private static void postJlink(Path JVM_DIR, Path LWJGL_DIR, Path JSON_DIR)
			throws IOException, InterruptedException {
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
			LAUNCHER_PROJECT_FILE.toString(),
			"-p:Platform=" + ARCH + ";Configuration=Release"
		).inheritIO().start().waitFor();
	}

	//TODO can use FileSystem API to do this as well
	private static void packageOutput(Path outFile) throws IOException {
		System.out.println("Zipping program to " + outFile);
		try(var zip  = new ZipOutputStream(Files.newOutputStream(outFile))) {
			zip.setComment("Sandbox2D Game Client Binaries");
			zip.setLevel(Deflater.BEST_COMPRESSION);
			zip.setMethod(ZipOutputStream.DEFLATED);
			Instant compileTime = Instant.now();
			traverse(OUTPUT_DIR, file -> {
				ZipEntry entry = new ZipEntry(OUTPUT_DIR.relativize(file).toString().replace('\\', '/'));
				entry.setCreationTime(FileTime.from(compileTime));
				entry.setLastModifiedTime(FileTime.from(compileTime));
				zip.putNextEntry(entry);
				zip.write(Files.readAllBytes(file));
				zip.closeEntry();
			}, path -> {});
		}
		System.out.println("Zipped to " + NumberFormat.getInstance().format(Files.size(outFile)) + " bytes.");
	}

	private static Iterable<? extends JavaFileObject> getSourceFiles(Path src) throws IOException {
		PathMatcher match = src.getFileSystem().getPathMatcher("glob:*.java");
		Path diagDir = src.getParent();
		return Files.find(src, Integer.MAX_VALUE, (path, attr) ->
			attr.isRegularFile() && match.matches(path.getFileName()))
			.peek(file -> System.out.println("FOUND " + diagDir.relativize(file)))
			.map(PathJavaFile::source)
			.collect(Collectors.toList());
	}

	private static boolean compile(Path output, Path diag, Iterable<? extends JavaFileObject> sources,
			Iterable<Path> modules) throws IOException {
		var javac = ToolProvider.getSystemJavaCompiler();
		try(var writer = new PrintWriter(System.err); var fileManager = new CustomFileManager(
			javac.getStandardFileManager(null, null, null), output, modules)) {
			return javac.getTask(
				writer,
				fileManager,
				createDiagnostic(diag),
				List.of(
					"-g:none",
					"--enable-preview",
					"--release", RELEASE_VERSION
					//"--module-path", join(modules, ';'),
					//"-d", output.toString()
				),
				null,
				sources
			).call();
		}
	}

	private static int jlink(Path output, Iterable<Path> modules, Iterable<String> includeModules) {
		return java.util.spi.ToolProvider.findFirst("jlink").orElseThrow().run(System.out, System.err,
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
//		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//			try {
//				traverse(temp, Files::delete, Files::delete);
//			} catch (IOException e) {
//				throw new RuntimeException("Error while deleting temp directory.", e);
//			}
//		}));
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

	private static final class PathJavaFile implements JavaFileObject {
		private final Path path;
		private final Kind kind;

		private static PathJavaFile source(Path path) {
			return new PathJavaFile(path, Kind.SOURCE);
		}

		private PathJavaFile(Path path, Kind kind) {
			this.path = path;
			this.kind = kind;
		}

		@Override
		public URI toUri() {
			return path.toAbsolutePath().normalize().toUri();
		}

		@Override
		public String getName() {
			return path.toString();
		}

		@Override
		public Kind getKind() {
			return kind;
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
		public OutputStream openOutputStream() throws IOException {
			if(kind == Kind.SOURCE)
				throw new UnsupportedOperationException("writes to source files not allowed");
			return Files.newOutputStream(path);
		}

		@Override
		public Writer openWriter() throws IOException {
			if(kind == Kind.SOURCE)
				throw new UnsupportedOperationException("writes to source files not allowed");
			return Files.newBufferedWriter(path);
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
			try {
				return Files.deleteIfExists(path);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		@Override
		public boolean isNameCompatible(String simpleName, Kind kind) {
			return kind == this.kind && path.getFileName().toString().equals(simpleName + kind.extension);
		}

		@Override
		public NestingKind getNestingKind() {
			return kind == Kind.SOURCE ? NestingKind.TOP_LEVEL : null;
		}

		@Override
		public Modifier getAccessLevel() {
			return null;
		}
	}

//	private static final class TestFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
//
//		/**
//		 * Creates a new instance of ForwardingJavaFileManager.
//		 *
//		 * @param fileManager delegate to this file manager
//		 */
//		protected TestFileManager(StandardJavaFileManager fileManager) {
//			super(fileManager);
//		}
//
//		@Override
//		public <S> ServiceLoader<S> getServiceLoader(Location location, Class<S> service) throws IOException {
//			var result = super.getServiceLoader(location, service);
//			System.out.println("get service loader for " + location + ": " + service + ": " + result);
//			return result;
//		}
//
//		@Override
//		public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
//			var result = super.getJavaFileForOutput(location, className, kind, sibling);
//			System.out.println("get java file for output " + result);
//			return result;
//		}
//
//		@Override
//		public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
//			var result = super.getFileForOutput(location, packageName, relativeName, sibling);
//			System.out.println("get file for output " + result);
//			return result;
//		}
//
//		@Override
//		public Location getLocationForModule(Location location, String moduleName) throws IOException {
//			var result = super.getLocationForModule(location, moduleName);
//			System.out.println("get location for module" + result);
//			return result;
//		}
//
//		@Override
//		public Location getLocationForModule(Location location, JavaFileObject fo) throws IOException {
//			var result = super.getLocationForModule(location, fo);
//			System.out.println("get location for module" + fo + " " + result);
//			return result;
//		}
//
//		@Override
//		public Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException {
//			var result = super.listLocationsForModules(location);
//			System.out.println("list mod locs for " + location);
//				for(var r : result) {
//					System.out.println("  " + r + " " + r.iterator().next().getClass().getName());
//				}
//			return result;
//		}
//
//		@Override
//		public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
//			var result = super.getJavaFileForInput(location, className, kind);
//			System.out.println("get java file for input" + result);
//			return result;
//		}
//
//		@Override
//		public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
//			var result = super.getFileForInput(location, packageName, relativeName);
//			System.out.println("get file for input" + result);
//			return result;
//		}
//
//
//	}

	//TODO write directly to a jar file to use up less space
	private static final class CustomFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
		private final Path output;

		private static final class ModuleLocation implements Location {
			private final Path file;

			ModuleLocation(Path file) {
				this.file = file;
			}

			@Override
			public String getName() {
				return file.toString();
			}

			@Override
			public boolean isOutputLocation() {
				return false;
			}

			@Override
			public boolean isModuleOrientedLocation() {
				return true;
			}
		}

		public CustomFileManager(StandardJavaFileManager fileManager, Path output, Iterable<Path> modules)
			throws IOException {
			super(fileManager);
			this.output = output;
			List<Set<Location>> collect = new ArrayList<>();
			for(Path module : modules) {
				ModuleLocation location = new ModuleLocation(module);
				collect.add(Set.of(location));
				//fileManager.setLocationFromPaths(location, List.of(module)); //TODO use module version?
			}

			List<Path> modulePath = new ArrayList<>();
			modules.forEach(modulePath::add);

			this.modules = collect;
			this.fm = fileManager;
			fileManager.setLocationFromPaths(StandardLocation.MODULE_PATH, modulePath);
		}

//		@Override
//		public boolean hasLocation(Location location) {
//			return location instanceof StandardLocation type && switch(type) {
//				case MODULE_PATH, CLASS_OUTPUT -> true;
//				default -> false;
//			};
//		}
//
//		@Override
//		public Iterable<Set<Location>> listLocationsForModules(Location location) {
//			System.out.println("List location for " + location);
//			if(location instanceof StandardLocation type) {
//				return switch(type) {
//					case MODULE_PATH -> modules;
//					default -> List.of();
//				};
//			} else {
//				throw new UnsupportedOperationException(
//					"Unsupported non-standard Location type " + location.getName());
//			}
//		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location,
			String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
			if(location instanceof StandardLocation type) {
				return switch(type) {
					default -> throw new UnsupportedOperationException(location + " " + className);
					case CLASS_OUTPUT -> {
						String classFormatted = className.replace('.', '/') + kind.extension;
						Path file = output.resolve(classFormatted);
						Files.createDirectories(file.getParent());
						yield new PathJavaFile(Files.createFile(file), kind);
					}
				};
			} else {
				throw new UnsupportedOperationException();
			}
		}

//		@Override
//		public boolean contains(Location location, FileObject fo) throws IOException {
//			throw new UnsupportedOperationException();
//		}
//
//		@Override
//		public ClassLoader getClassLoader(Location location) {
//			return null;
//		}
//
//		@Override
//		public Iterable<JavaFileObject> list(Location location, String packageName,
//			Set<JavaFileObject.Kind> kinds, boolean recurse) {
//			System.out.println("list " + location);
//			return List.of();
//		}
//
//		@Override
//		public String inferModuleName(Location location) throws IOException {
//			throw new UnsupportedOperationException("non-modular libraries not supported");
//		}
//
//		@Override
//		public String inferBinaryName(Location location, JavaFileObject file) {
//			throw new UnsupportedOperationException();
//		}
//
//		@Override
//		public boolean isSameFile(FileObject a, FileObject b) {
//			throw new UnsupportedOperationException();
//		}
//
//		@Override
//		public boolean handleOption(String current, Iterator<String> remaining) {
//			System.out.println("handle option " + current);
//			return false;
//		}
//
//		@Override
//		public Location getLocationForModule(Location location, String moduleName) throws IOException {
//			throw new UnsupportedOperationException();
//		}
//
//		@Override
//		public Location getLocationForModule(Location location, JavaFileObject fo) throws IOException {
//			throw new UnsupportedOperationException();
//		}
//
//		@Override
//		public JavaFileObject getJavaFileForInput(Location location,
//			String className, JavaFileObject.Kind kind) {
//			System.out.println("getJavaFileForInput");
//			return null;
//		}
//
//		@Override
//		public FileObject getFileForInput(Location location,
//			 String packageName, String relativeName) {
//			throw new UnsupportedOperationException();
//		}
//
//		@Override
//		public FileObject getFileForOutput(Location location,
//			 String packageName, String relativeName, FileObject sibling) {
//			throw new UnsupportedOperationException();
//		}
//
//		@Override
//		public int isSupportedOption(String option) {
//			return -1;
//		}
//
//		@Override
//		public void flush() {
//
//		}
//
//		@Override
//		public void close() {
//
//		}
	}
}