package ritzow.sandbox.client.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Queue;

public class CompileShaders {
	private static final Path DEST = Path.of("resources/shaders/new/out");

	private static final boolean DEBUG = true;

	private static record CompileTask(Process process, String name) {}

	public static void main(String... args) throws IOException, InterruptedException {
		Queue<CompileTask> processes = new ArrayDeque<>();
		for(Path file : Files.newDirectoryStream(Path.of("resources/shaders/new"), Files::isRegularFile)) {
			Path destFile = DEST.resolve(file.getFileName() + ".spv");
			if(Files.notExists(destFile) || Files.getLastModifiedTime(file).compareTo(Files.getLastModifiedTime(destFile)) > 0) {
				processes.add(build(file, destFile));
			}
		}
		while(!processes.isEmpty()) {
			CompileTask next = processes.poll();
			System.out.printf("%30s compilation exited with value %d%n", next.name, next.process.waitFor());
		}
	}

	private static CompileTask build(Path file, Path dest) throws IOException {
		//https://www.khronos.org/opengles/sdk/tools/Reference-Compiler/
		//TODO look into -l option "link all input files together to form a single module" to validate glsl programs and create program files
		ProcessBuilder builder = DEBUG ? new ProcessBuilder(
			"glslangValidator",
			"-G460",
			"-Od",
			"-t",
			"-H", //print opcodes
			"-g", //print debug info
			"-e", "main",
			"-o", dest.toString(),
			file.toString()
		) : new ProcessBuilder(
			"glslangValidator",
			"-G460",
			"--quiet",
			"-Os",
			"-t",
			DEBUG ? "-g" : "-g0", //strip debug info
			"-e", "main",
			"-o", dest.toString(),
			file.toString()
		);

		return new CompileTask(builder.inheritIO().start(), file.getFileName().toString());
	}
}
