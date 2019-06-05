package ritzow.sandbox.client;

import java.util.ArrayList;
import java.util.List;

public class GameLoop {
	private static final List<GameTask> updates = new ArrayList<>();
	private static int index;
	
	public static interface GameTask {
		void run() throws Exception;
	}
	
	public static void print() {
		for(int i = 0; i < updates.size(); i++) {
			System.out.print(updates.get(i));
			if(i == index)
				System.out.print("<-- current");
			System.out.println();
		}
	}
	
	public static void run(GameTask... initial) throws Exception {
		for(var task : initial) {
			updates.add(task);
		}
		
		while(!updates.isEmpty()) {
			for(index = 0; index < updates.size(); index++) {
				updates.get(index).run();
			}
		}
	}
	
	public static void set(GameTask... tasks) {
		index = 0;
		updates.clear();
		for(var task : tasks) {
			updates.add(task);
		}
	}
}
