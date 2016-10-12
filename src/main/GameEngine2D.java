package main;

public class GameEngine2D {
	public static void main(String[] args) {
		EventManager eventManager = new EventManager();
		System.out.println(System.getProperty("user.dir"));
		new Thread(new GameManager(eventManager), "Game Manager").start();
		eventManager.run();
	}
}