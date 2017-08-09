package ritzow.sandbox.client.input.controller;

import ritzow.sandbox.client.input.handler.InputHandler;

/** Represents a user input system such as player movement or mouse picking **/
public interface Controller extends InputHandler, Runnable {
	public default void run() {
		this.update();
	}
	
	public void update();
}
