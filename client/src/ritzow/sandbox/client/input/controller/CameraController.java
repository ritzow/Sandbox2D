package ritzow.sandbox.client.input.controller;

import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.EventDelegator;
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

	public void link(EventDelegator manager) {
		manager.keyboardHandlers().add(this);
		manager.scrollHandlers().add(this);
		manager.mouseButtonHandlers().add(this);
	}

	public void unlink(EventDelegator manager) {
		manager.keyboardHandlers().remove(this);
		manager.scrollHandlers().remove(this);
		manager.mouseButtonHandlers().remove(this);
	}

}
