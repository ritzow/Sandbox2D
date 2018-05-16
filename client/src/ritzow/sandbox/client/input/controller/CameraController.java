package ritzow.sandbox.client.input.controller;

import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.input.handler.MouseButtonHandler;
import ritzow.sandbox.client.input.handler.ScrollHandler;

public abstract class CameraController implements Controller, KeyHandler, ScrollHandler, MouseButtonHandler {
	protected final Camera camera;
	
	public CameraController(Camera camera) {
		this.camera = camera;
	}
	
	public Camera getCamera() {
		return camera;
	}

	public void link(InputManager manager) {
		manager.getKeyHandlers().add(this);
		manager.getScrollHandlers().add(this);
		manager.getMouseButtonHandlers().add(this);
	}

	public void unlink(InputManager manager) {
		manager.getKeyHandlers().remove(this);
		manager.getScrollHandlers().remove(this);
		manager.getMouseButtonHandlers().remove(this);
	}

}
