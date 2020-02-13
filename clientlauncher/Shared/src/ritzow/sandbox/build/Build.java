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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

public class Build {
	private static final PathMatcher SRC_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.java");
	private static final String RELEASE_VERSION = Integer.toString(Runtime.version().feature());
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	
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
	
	public static void main(String... args) throws IOException {
		Path SHARED_DIR = Path.of(args[0]);
		Path CLIENT_DIR = Path.of(args[1]);
		Path OUTPUT_DIR = Path.of(args[2]);

		Path temp = Files.createTempDirectory(OUTPUT_DIR, "classes_");
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				delete(temp);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}));
		
		Path sharedOut = temp.resolve("shared");
		Path sharedSrc = SHARED_DIR.resolve("src");
		System.out.println("Searching for shared code...");
		var sharedFiles = getSourceFiles(sharedSrc);
		System.out.println("Compiling shared code...");
		boolean sharedSuccess = compile(
			sharedOut, 
			sharedSrc, 
			sharedFiles, 
			List.of()
		);
		
		if(sharedSuccess) {
			System.out.println("Shared code compiled.");
			Collection<Path> modules = LWJGL_MODULES.stream()
				.map(CLIENT_DIR.resolve("libraries").resolve("lwjgl")::resolve)
				.collect(Collectors.toList());
			modules.add(sharedOut);
			Path clientSrc = CLIENT_DIR.resolve("src");
			Path clientOut = temp.resolve("client");
			System.out.println("Searching for client code...");
			var clientFiles = getSourceFiles(clientSrc);
			System.out.println("Compiling client code...");
			boolean clientSuccess = compile(
				clientOut, 
				clientSrc,
				clientFiles, 
				modules
			);
			
			if(clientSuccess) {
				System.out.print("Client code compiled.\nRunning jlink...");
				modules.add(clientOut);
				int result = jlink(OUTPUT_DIR.resolve("jvm"), MODULE_NAMES, modules);
				System.out.println(result == 0 ? "jlink successful." : "jlink failed with error " + result + ".");
			} else {
				System.out.println("Client compilation failed.");
			}
		} else {
			System.out.println("Shared compilation failed.");
		}
	}
	
	private static Iterable<JavaFileObject> getSourceFiles(Path src) throws IOException {
		return Files.walk(src)
			.filter(file -> !Files.isDirectory(file) && SRC_MATCHER.matches(file.getFileName()))
			.peek(file -> System.out.println("ADDED " + src.relativize(file)))
			.map(PathSourceFile::new)
			.collect(Collectors.toList());
	}
	
	private static boolean compile(Path output, Path diag, Iterable<JavaFileObject> sources, 
			Collection<Path> modules) {
		DiagnosticListener<JavaFileObject> listener = diagnostic -> {
			StringBuilder msg = new StringBuilder(diagnostic.getKind().name()).append(" ");
			if(diagnostic.getSource() == null) {
				msg.append(diagnostic.getMessage(Locale.getDefault()));
			} else {
				msg.append(diag.relativize(Path.of(diagnostic.getSource().getName())));
				if(diagnostic.getLineNumber() != Diagnostic.NOPOS) {
					msg.append(":" + diagnostic.getLineNumber());
				}
				msg.append("\n\t").append(diagnostic.getMessage(Locale.getDefault()));
			}
			System.out.println(msg);
		};
		
		return javax.tools.ToolProvider.getSystemJavaCompiler().getTask(
			null,
			null,
			listener,
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
	
	private static void delete(Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		        if (exc != null)
		            throw exc;
				Files.delete(dir);
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
			return null;
		}

		@Override
		public Modifier getAccessLevel() {
			return null;
		}
	}
}