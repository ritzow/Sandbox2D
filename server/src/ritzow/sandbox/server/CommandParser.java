package ritzow.sandbox.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class CommandParser implements Runnable {
	private static final Pattern nameMatcher = Pattern.compile("\\w+");
	private static final CommandEntry UNKNOWN_COMMAND = 
			new CommandEntry(args -> System.out.println("Unknown command."), false);
	
	private final Map<String, CommandEntry> commands;
	private final Queue<Runnable> commandQueue;
	
	private static final class CommandEntry {
		final Consumer<String> action;
		final boolean terminateParser;
		
		private CommandEntry(Consumer<String> action, boolean terminateParser) {
			super();
			this.action = action;
			this.terminateParser = terminateParser;
		}
	}
	
	public CommandParser() {
		commands = new HashMap<>();
		commandQueue = new ConcurrentLinkedQueue<>();
	}
	
	public CommandParser register(String name, Consumer<String> action, boolean terminateParser) {
		if(commands.putIfAbsent(name.toLowerCase(), new CommandEntry(action, terminateParser)) != null)
			throw new IllegalArgumentException(name + " already registered");
		return this;
	}

	@Override
	public void run() {
		System.out.println("Available Commands: (" + String.join(", ", commands.keySet()) + ")");
		try(var scan = new Scanner(System.in)) {
			CommandEntry entry;
			do {
				entry = scan.hasNext(nameMatcher) ? 
					commands.getOrDefault(scan.next(nameMatcher).toLowerCase(), UNKNOWN_COMMAND) :
					UNKNOWN_COMMAND;
				Consumer<String> command = entry.action;
				String args = scan.nextLine().strip();
				commandQueue.add(() -> command.accept(args));
			} while(!entry.terminateParser);
		}
	}

	public void update() {
		while(!commandQueue.isEmpty()) {
			commandQueue.remove().run();
		}
	}
}
