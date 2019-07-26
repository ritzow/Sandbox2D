package ritzow.sandbox.client;

import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.util.Utility;

public class GameLoop {
	private static GameContext current;

	public static void start(GameContext initial) {
		if(current != null)
			throw new IllegalStateException("GameLoop already started");
		current = initial;
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