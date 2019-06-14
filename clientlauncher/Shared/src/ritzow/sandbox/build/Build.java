package ritzow.sandbox.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;

public class Build {
	public static final Path SHARED_DIR = Path.of("../../shared");
	public static final Path CLIENT_DIR = Path.of("../../client");
	public static final Path LIBRARY_DIR = CLIENT_DIR.resolve("libraries");
	public static final Path SOURCE_DIR = CLIENT_DIR.resolve("src");
	public static final Path OUTPUT_DIR = Path.of("built-bin");
	
	public static void main(String... args) throws IOException {
		if(Files.notExists(OUTPUT_DIR))
			Files.createDirectory(OUTPUT_DIR);
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		try(var fileManager = compiler.getStandardFileManager(System.err::print, Locale.ENGLISH, StandardCharsets.UTF_8)) {
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.java");
			List<Path> classFiles = new ArrayList<Path>();
			Files.walk(SOURCE_DIR).forEach(file -> {
				System.out.print(file);
				if(!Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS) && matcher.matches(file.getFileName())) {
					System.out.print(" ADDED");
					classFiles.add(file);
				}
				System.out.println();
			});
			
			Path LWJGL = LIBRARY_DIR.resolve("lwjgl");
			String[] libraries = {
				LWJGL.resolve("lwjgl.jar").toString(),
				LWJGL.resolve("lwjgl-glfw.jar").toString(),
				LWJGL.resolve("lwjgl-opengl.jar").toString(),
				LWJGL.resolve("lwjgl-openal.jar").toString(),
				LIBRARY_DIR.resolve("PNGDecoder").resolve("PNGDecoder.jar").toString(),
				SHARED_DIR.resolve("bin").toString()
			};
			
			List<String> options = List.of(
				"-d", OUTPUT_DIR.toString(),
				"--enable-preview",
				"--release", "12",
				"--module-path", String.join(";", libraries),
				"-Xlint:preview"
			);
			
			DiagnosticListener<JavaFileObject> listener = message -> {
				switch(message.getKind()) {
					case ERROR, WARNING, MANDATORY_WARNING, OTHER -> {
						System.err.println(message);
					}
					case NOTE -> {
						System.out.println(message.getMessage(Locale.ENGLISH));
					}
					default -> throw new UnsupportedOperationException("Unknown enum value " + message.getKind());
				}
			};

			var files = fileManager.getJavaFileObjectsFromPaths(classFiles);
			CompilationTask task = compiler.getTask(null, fileManager, listener, options, null, files);	
			task.call();
		}
	}
}