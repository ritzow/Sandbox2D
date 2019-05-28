package ritzow.sandbox.client;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class GameLoop {
	private static List<GameTask> updates = new ArrayList<>();
	private static ListIterator<GameTask> currentTasks;
	
	public static interface GameTask {
		void run() throws Exception;
	}
	
	public static void run() throws Exception {
		while(!updates.isEmpty()) {
			currentTasks = updates.listIterator();
			while(currentTasks.hasNext()) {
				currentTasks.next().run();
			}
		}
	}
	
	public static void addTask(GameTask task) {
		updates.add(task);
	}
	
	public static void endTask() {
		currentTasks.remove();
	}

	public static void replaceTask(GameTask task) {
		endTask();
		spawnTask(task);
	}

	public static void spawnTask(GameTask task) {
		currentTasks.add(task);
	}
}
