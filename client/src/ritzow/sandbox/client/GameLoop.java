package ritzow.sandbox.client;

import java.util.Objects;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.util.Utility;

import static ritzow.sandbox.client.data.StandardClientOptions.FRAME_TIME_LIMIT;
import static ritzow.sandbox.client.data.StandardClientOptions.LIMIT_FPS;

class GameLoop {
	public interface GameContext {
		void run(long deltaNanos) throws Exception;
	}

	private static GameContext current;

	private static long lastUpdate;

	public static void start(GameContext initial) throws Exception {
		if(current != null)
			throw new IllegalStateException("GameLoop already started");
		current = Objects.requireNonNull(initial);
		lastUpdate = System.nanoTime();
		if(StandardClientOptions.PRINT_FPS) {
			while(current != null) {
				long frameStart = System.nanoTime();
				current.run(frameStart - lastUpdate);
				lastUpdate = Math.max(frameStart, lastUpdate);
				if(LIMIT_FPS) Utility.limitFramerate(frameStart, FRAME_TIME_LIMIT);
				Utility.printFramerate(frameStart);
			}
		} else {
			while(current != null) {
				long frameStart = System.nanoTime();
				current.run(frameStart - lastUpdate);
				lastUpdate = Math.max(frameStart, lastUpdate);
				if(LIMIT_FPS) Utility.limitFramerate(frameStart, FRAME_TIME_LIMIT);
			}
		}
	}

	public static long updateDeltaTime() {
		long lastTime = lastUpdate;
		return (lastUpdate = System.nanoTime()) - lastTime;
	}

	public static void setContext(GameContext context) {
		current = context;
	}

	public static void stop() {
		current = null;
	}
}