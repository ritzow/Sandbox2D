package ritzow.sandbox.client;

public class GameLoop {
	private static GameContext current;

	public static void start(GameContext initial) {
		if(current != null)
			throw new IllegalStateException("GameLoop already started");
		current = initial;
		while(current != null) {
			current.run();
		}
	}

	public static void setContext(GameContext context) {
		current = context;
	}

	public static void stop() {
		current = null;
	}
}