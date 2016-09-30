package main;

public class GameEngine2D {
	public static void main(String[] args) {
		System.setProperty("java.library.path", "libraries/natives/");
		System.setProperty("org.lwjgl.librarypath", "libraries/natives/");
		EventManager eventManager = new EventManager();
		new Thread(new GameManager(eventManager), "Game Manager").start();
		eventManager.run();
	}
}