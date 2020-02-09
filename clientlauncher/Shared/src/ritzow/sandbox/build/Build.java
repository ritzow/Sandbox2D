package ritzow.sandbox.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.spi.ToolProvider;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

public class Build {
	private static final PathMatcher SRC_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.java");
	private static final String RELEASE_VERSION = Integer.toString(Runtime.version().feature());
	
	private static final String[] MODULES = {
		"java.base",
		"ritzow.sandbox.client",
		"ritzow.sandbox.shared",
		"jdk.unsupported",
		"org.lwjgl",
		"org.lwjgl.glfw",
		"org.lwjgl.opengl",
		"org.lwjgl.openal"
	};
	
	public static void main(String... args) throws IOException {
		Path SHARED_DIR = Path.of(args[0]);
		Path CLIENT_DIR = Path.of(args[1]);
		Path OUTPUT_DIR = Path.of(args[2]);
		Path LIBRARY_DIR = CLIENT_DIR.resolve("libraries");
		Path LWJGL_DIR = LIBRARY_DIR.resolve("lwjgl");
		
		System.out.println("Collecting and compiling classes...");
		
		//Search for all the java files to compile and put them in a list
		Path[] classFiles = Files.walk(CLIENT_DIR.resolve("src"))
			.filter(file -> !Files.isDirectory(file) && SRC_MATCHER.matches(file.getFileName()))
			.peek(file -> System.out.println("ADDED " + CLIENT_DIR.relativize(file)))
			.toArray(Path[]::new);
		
		String[] libraries = {
			LWJGL_DIR.resolve("lwjgl.jar").toString(),
			LWJGL_DIR.resolve("lwjgl-glfw.jar").toString(),
			LWJGL_DIR.resolve("lwjgl-opengl.jar").toString(),
			LWJGL_DIR.resolve("lwjgl-openal.jar").toString(),
			SHARED_DIR.resolve("bin").toString()
		};
		
		Path OUTPUT_TEMP = Files.createTempDirectory(OUTPUT_DIR, "bin");		
	
		if(compile(OUTPUT_TEMP, CLIENT_DIR, classFiles, libraries)) {
			System.out.print("Compilation successful.\nRunning jlink...");
			int result = jlink(OUTPUT_TEMP, OUTPUT_DIR, libraries);
			System.out.println(result == 0 ? "jlink successful." : "jlink failed with error " + result + ".");
		} else {
			System.out.println("Compilation failed.");
		}
		delete(OUTPUT_TEMP);
	}
	
	private static boolean compile(Path temp, Path clientDir,
			Path[] classes, String[] modules) throws IOException {
		DiagnosticListener<JavaFileObject> LISTENER = diagnostic -> {
			StringBuilder msg = new StringBuilder(diagnostic.getKind().name()).append(" ");
			if(diagnostic.getSource() == null) {
				msg.append(diagnostic.getMessage(Locale.getDefault()));
			} else {
				msg.append(clientDir.relativize(Path.of(diagnostic.getSource().getName())));
				if(diagnostic.getLineNumber() != Diagnostic.NOPOS) {
					msg.append(":" + diagnostic.getLineNumber());
				}
				msg.append("\n\t").append(diagnostic.getMessage(Locale.getDefault()));
			}
			System.out.println(msg);
		};
		
		JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
		try(StandardJavaFileManager fileManager = 
			compiler.getStandardFileManager(System.err::print, null, StandardCharsets.UTF_8)) {
			return compiler.getTask(
					null,
					fileManager,
					LISTENER,
					List.of(
						"--enable-preview",
						"--release", RELEASE_VERSION,
						"--module-path", String.join(";", modules),
						"-d", temp.toString()
					),
					null,
					fileManager.getJavaFileObjects(classes)
			).call();
		}
	}
	
	private static int jlink(Path temp, Path output, String... libraries) {
		return ToolProvider.findFirst("jlink").get().run(System.out, System.err, 
			"--compress", "2",
			"--no-man-pages",
			"--endian", "little",
			"--strip-debug",
			"--module-path", String.join(";", libraries).concat(";").concat(temp.toString()),
			"--add-modules", String.join(",", MODULES),
			"--output", output.resolve("jvm").toString()
		);
	}
	
	private static void delete(Path file) throws IOException {
		Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
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