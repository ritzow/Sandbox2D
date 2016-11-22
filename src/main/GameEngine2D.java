package main;

import input.EventManager;

public class GameEngine2D {
	
	public static final boolean PRINT_MEMORY_USAGE = false;
	
	public static void main(String[] args) throws InterruptedException {
		EventManager eventManager = new EventManager();
		//System.out.println("Working directory: " + System.getProperty("user.dir"));
		new Thread(new GameManager(eventManager), "Game Manager").start();
		eventManager.run();
	}
}