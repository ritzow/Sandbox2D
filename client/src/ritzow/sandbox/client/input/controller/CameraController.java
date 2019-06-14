package ritzow.sandbox.client.input.controller;

import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.input.handler.MouseButtonHandler;
import ritzow.sandbox.client.input.handler.ScrollHandler;

public abstract class CameraController implements KeyHandler, ScrollHandler, MouseButtonHandler {
	protected final Camera camera;

	public CameraController(Camera camera) {
		this.camera = camera;
	}

	public Camera getCamera() {
		return camera;
	}
}
