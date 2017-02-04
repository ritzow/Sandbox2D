package ritzow.solomon.engine.main;

import ritzow.solomon.engine.input.EventProcessor;

public class GameEngine2D {
	public static void main(String[] args) {
		EventProcessor eventManager = new EventProcessor();
		new Thread(new GameManager(eventManager), "Game Manager").start();
		eventManager.run();
	}
}