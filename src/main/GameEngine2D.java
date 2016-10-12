package main;

public class GameEngine2D {
	public static void main(String[] args) {
		System.setProperty("org.lwjgl.librarypath", "libraries/archives/lwjgl/natives");
		System.setProperty("java.library.path", "libraries/archives/lwjgl/natives");
		EventManager eventManager = new EventManager();
		new Thread(new GameManager(eventManager), "Game Manager").start();
		eventManager.run();
	}
}