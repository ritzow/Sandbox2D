package ritzow.sandbox.client.input.controller;

/** Represents a user input system such as player movement or mouse picking **/
public interface Controller extends Runnable {
	
	@Override
	public default void run() {
		this.update();
	}
	
	public void update();
}
