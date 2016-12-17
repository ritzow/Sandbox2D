package main;

import input.EventManager;

public class GameEngine2D {
	
	static final boolean PRINT_MEMORY_USAGE = false;
	
	public static void main(String[] args) {
		EventManager eventManager = new EventManager();
		new Thread(new GameManager(eventManager), "Game Manager").start();
		eventManager.run();
	}
}