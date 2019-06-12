package ritzow.sandbox.client;

import java.util.ArrayDeque;
import java.util.Queue;
import ritzow.sandbox.client.StartClient.GameState;

public class GameLoop {
	//private static GameTask task;
	private static final Queue<GameState> tasks = new ArrayDeque<>();
	
	public static void start(GameState initial) throws Exception {
		//task = initial;
		tasks.add(initial);
//		while(task != null) {
//			task.run();
//		}
		while(!tasks.isEmpty()) {
			tasks.peek().run();
		}
	}
	
	public static void end() {
		//task = null;
		tasks.poll();
	}
	
	public static void set(GameState task) {
		//GameLoop.task = task;
		tasks.add(task);
	}
}
