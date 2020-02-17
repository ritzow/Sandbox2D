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
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.spi.ToolProvider;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

public class Build {
	private static final String RELEASE_VERSION = Integer.toString(Runtime.version().feature());
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private static final String OS = "windows", ARCH = "x64";
	
	private static final String[] MODULE_NAMES = {
		"java.base",
		"ritzow.sandbox.client",
		"ritzow.sandbox.shared",
		"jdk.unsupported",
		"org.lwjgl",
		"org.lwjgl.glfw",
		"org.lwjgl.opengl",
		"org.lwjgl.openal"
	};
	
	private static final List<Path> LWJGL_MODULES = List.of(
		Path.of("lwjgl.jar"),
		Path.of("lwjgl-glfw.jar"),
		Path.of("lwjgl-opengl.jar"),
		Path.of("lwjgl-openal.jar")
	);
	
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
		
		Thread.sleep(100);
		Files.createDirectory(OUTPUT_DIR);
		Path temp = Files.createTempDirectory(OUTPUT_DIR, "classes_");
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				traverse(temp, Files::delete, Files::delete);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}));
		
		Path sharedOut = temp.resolve("shared");
		Path sharedSrc = SHARED_DIR.resolve("src");
		System.out.println("Searching for shared code.");
		var sharedFiles = getSourceFiles(sharedSrc);
		System.out.println("Compiling shared code.");
		boolean sharedSuccess = compile(
			sharedOut, 
			sharedSrc, 
			sharedFiles, 
			List.of()
		);
		
		if(sharedSuccess) {
			Path LWJGL_DIR = CLIENT_DIR.resolve("libraries").resolve("lwjgl");
			System.out.println("Shared code compiled.");
			Collection<Path> modules = LWJGL_MODULES.stream()
				.map(LWJGL_DIR::resolve)
				.collect(Collectors.toList());
			modules.add(sharedOut);
			Path clientSrc = CLIENT_DIR.resolve("src");
			Path clientOut = temp.resolve("client");
			System.out.println("Searching for client code.");
			var clientFiles = getSourceFiles(clientSrc);
			System.out.println("Compiling client code.");
			boolean clientSuccess = compile(
				clientOut, 
				clientSrc,
				clientFiles, 
				modules
			);
			
			if(clientSuccess) {
				System.out.print("Client code compiled.\nRunning jlink... ");
				modules.add(clientOut);
				Path JVM_DIR = OUTPUT_DIR.resolve("jvm");
				int result = jlink(JVM_DIR, MODULE_NAMES, modules);
				if(result == 0) {
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
					Files.copy(CLIENT_DIR.resolve("libraries").resolve("json").resolve("LICENSE"), 
							LEGAL_DIR.resolve("json_license.txt"));
					
					//run launcher executable build script
					System.out.println("done.\nBuilding launcher executable.");
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
		//List<JavaFileObject> list = new ArrayList<JavaFileObject>();
		return Files.find(src, Integer.MAX_VALUE, (path, attr) -> 
				!attr.isDirectory() && match.matches(path.getFileName()))
			.peek(file -> System.out.println("FOUND " + diagDir.relativize(file)))
			.map(PathSourceFile::new)
			//.forEach(list::add);
			.collect(SourceCollector.COLLECTOR);
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
	
	private static boolean compile(Path output, Path diag, Iterable<? extends JavaFileObject> sources, 
			Collection<Path> modules) {
		return javax.tools.ToolProvider.getSystemJavaCompiler().getTask(
			null,
			null,
			createDiagnostic(diag),
			List.of(
			"--enable-preview",
			"--release", RELEASE_VERSION,
			"--module-path", modules.stream().map(Path::toString).collect(Collectors.joining(";")),
			"-d", output.toString()
			),
			null,
			sources
		).call();
	}
	
	private static int jlink(Path output, String[] moduleNames, Collection<Path> modules) {
		return ToolProvider.findFirst("jlink").get().run(System.out, System.err, 
			"--compress", "2",
			"--no-man-pages",
			"--endian", "little",
			//"--strip-debug",
			"--module-path", modules.stream().map(Path::toString).collect(Collectors.joining(";")),
			"--add-modules", String.join(",", moduleNames),
			"--output", output.toString()
		);
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
	
	private static final class SourceCollector implements 
			Collector<PathSourceFile, ArrayList<PathSourceFile>, List<PathSourceFile>> {
		public static final SourceCollector COLLECTOR = new SourceCollector();
		private static final Set<Characteristics> chars = 
				Set.of(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH);
		
		@Override
		public Supplier<ArrayList<PathSourceFile>> supplier() {
			return ArrayList<PathSourceFile>::new;
		}

		@Override
		public BiConsumer<ArrayList<PathSourceFile>, PathSourceFile> accumulator() {
			return ArrayList::add;
		}

		@Override
		public BinaryOperator<ArrayList<PathSourceFile>> combiner() {
			return (left, right) -> {
				left.addAll(right);
				return left;
			};
		}

		@Override
		public Function<ArrayList<PathSourceFile>, List<PathSourceFile>> finisher() {
			return arg -> arg;
		}

		@Override
		public Set<Characteristics> characteristics() {
			return chars;
		}
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
			return Files.newBufferedReader(path, CHARSET);
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			return Files.readString(path, CHARSET);
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