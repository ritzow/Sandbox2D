package ritzow.sandbox.client;

import java.util.Objects;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.util.Utility;

public class GameLoop {
	public interface GameContext {
		void run() throws Exception;
	}
	
	private static GameContext current;

	public static void start(GameContext initial) throws Exception {
		if(current != null)
			throw new IllegalStateException("GameLoop already started");
		current = Objects.requireNonNull(initial);
		if(StandardClientOptions.PRINT_FPS) {
			while(current != null) {
				long start = System.nanoTime();
				current.run();
				Utility.printFramerate(start);
			}
		} else {
			while(current != null) {
				current.run();
			}
		}
	}

	public static void setContext(GameContext context) {
		current = context;
	}

	public static void stop() {
		current = null;
	}
}