package ritzow.sandbox.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;

public class Build {
	private static final PathMatcher SRC_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.java");
	private static final String RELEASE_VERSION = Integer.toString(Runtime.version().feature());
	
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
	
	private static final Path TEMP_SHARED = Path.of("shared");
	private static final Path TEMP_CLIENT = Path.of("client");
	
	public static void main(String... args) throws IOException {
		Path SHARED_DIR = Path.of(args[0]);
		Path CLIENT_DIR = Path.of(args[1]);
		Path OUTPUT_DIR = Path.of(args[2]);
		Path LWJGL_DIR = CLIENT_DIR.resolve("libraries").resolve("lwjgl");
		
		System.out.println("Collecting and compiling classes...");
		
		Path temp = Files.createTempDirectory(OUTPUT_DIR, "temp");
		
		System.out.println("Compiling shared code...");
		if(compile(temp.resolve(TEMP_SHARED), SHARED_DIR, getSourceFiles(SHARED_DIR.resolve("src")), List.of())) {
			System.out.println("Shared code compiled.");
			System.out.println("Compiling client code...");
			
			Collection<Path> modules = new ArrayList<Path>();
			
			for(Path p : LWJGL_MODULES) {
				modules.add(LWJGL_DIR.resolve(p));
			}
			
			modules.add(temp.resolve(TEMP_SHARED));
			
			if(compile(temp.resolve(TEMP_CLIENT), CLIENT_DIR, getSourceFiles(CLIENT_DIR.resolve("src")), modules)) {
				System.out.print("Client code compiled.\nRunning jlink...");
				modules.add(temp.resolve(TEMP_CLIENT));
				int result = jlink(OUTPUT_DIR.resolve("jvm"), MODULE_NAMES, modules);
				System.out.println(result == 0 ? "jlink successful." : "jlink failed with error " + result + ".");
			} else {
				System.out.println("Compilation failed.");
			}
		}
		delete(temp);
	}
	
	private static Path[] getSourceFiles(Path src) throws IOException {
		return Files.walk(src)
			.filter(file -> !Files.isDirectory(file) && SRC_MATCHER.matches(file.getFileName()))
			.peek(file -> System.out.println("ADDED " + src.relativize(file)))
			.toArray(Path[]::new);
	}
	
	private static String listModules(Collection<Path> modules) {
		return modules.stream().map(Path::toString).collect(Collectors.joining(";"));
	}
	
	private static boolean compile(Path output, Path diag, Path[] sources, Collection<Path> modules) throws IOException {
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
		
		JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
		try(var fileManager = compiler.getStandardFileManager(System.err::print, null, StandardCharsets.UTF_8)) {
			return compiler.getTask(
				null,
				fileManager,
				listener,
				List.of(
				"--enable-preview",
				"--release", RELEASE_VERSION,
				"--module-path", listModules(modules),
				"-d", output.toString()
				),
				null,
				fileManager.getJavaFileObjects(sources)
			).call();
		}
	}
	
	private static int jlink(Path output, String[] moduleNames, Collection<Path> modules) {
		return ToolProvider.findFirst("jlink").get().run(System.out, System.err, 
			"--compress", "2",
			"--no-man-pages",
			"--endian", "little",
			//"--strip-debug",
			"--module-path", listModules(modules),
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
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}