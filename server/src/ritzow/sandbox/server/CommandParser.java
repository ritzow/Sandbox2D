package ritzow.sandbox.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class CommandParser {
	private final Map<String, Consumer<String>> commands = new HashMap<>();
	private final Queue<Runnable> commandQueue = new ConcurrentLinkedQueue<>();
	private volatile boolean run = true;
	private static final Pattern nameMatcher = Pattern.compile("\\w+");

	public void register(String name, Consumer<String> action) {
		if(commands.putIfAbsent(name, action) != null)
			throw new IllegalArgumentException(name + " already registered");
	}

	public void run() {
		System.out.println("Enter commands (" + String.join(", ", commands.keySet()) + "): ");
		try(var scan = new Scanner(System.in)) {
			while(run) {
				Consumer<String> command = getCommand(scan.next(nameMatcher).toLowerCase());
				String args = scan.nextLine().stripLeading();
				commandQueue.add(() -> command.accept(args));
			}
		}
	}

	public void update() {
		while(!commandQueue.isEmpty()) {
			commandQueue.remove().run();
		}
	}

	public void quit() {
		run = false;
	}

	public Consumer<String> getCommand(String name) {
		return commands.getOrDefault(name, CommandParser::unknownCommand);
	}

	private static void unknownCommand(String args) {
		System.out.println("Unknown command.");
	}
}
