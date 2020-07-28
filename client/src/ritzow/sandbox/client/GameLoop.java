package ritzow.sandbox.client;

import java.util.Objects;
import ritzow.sandbox.util.Utility;

import static ritzow.sandbox.client.data.StandardClientOptions.*;

class GameLoop {
	public interface GameContext {
		void run(long deltaNanos);
	}

	private static GameContext current;

	private static long lastUpdate, lastUpdateTime;

	public static void start(GameContext initial) {
		if(current != null)
			throw new IllegalStateException("GameLoop already started");
		current = Objects.requireNonNull(initial);
		lastUpdate = System.nanoTime();
		while(current != null) {
			long frameStart = System.nanoTime();
			long delta = frameStart - lastUpdate;
			current.run(delta);
			lastUpdate = Math.max(frameStart, lastUpdate);
			lastUpdateTime = delta;
			if(LIMIT_FPS) Utility.limitFramerate(frameStart, FRAME_TIME_LIMIT);
			if(PRINT_FPS) Utility.printFramerate(frameStart);
		}
	}

	public static long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public static long updateDeltaTime() {
		long lastTime = lastUpdate;
		return (lastUpdate = System.nanoTime()) - lastTime;
	}

	public static void setContext(Runnable context) {
		current = delta -> context.run();
	}

	public static void setContext(GameContext context) {
		current = context;
	}

	public static void stop() {
		current = null;
	}
}