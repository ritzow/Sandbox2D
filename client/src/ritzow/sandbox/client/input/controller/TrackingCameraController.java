package ritzow.sandbox.client.input.controller;

import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.handler.ScrollHandler;
import ritzow.sandbox.world.entity.Entity;

public final class TrackingCameraController implements ScrollHandler {
	private final Camera camera;
	private final float zoomSpeed, minZoom, maxZoom;

	public TrackingCameraController(float zoomSpeed, float minZoom, float maxZoom) {
		this.camera = new Camera(0, 0, minZoom);
		this.zoomSpeed = zoomSpeed;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
	}

	public void update(Display display, Entity target, AudioSystem audio, long nanoseconds) {
		if(display.isControlActivated(Control.RESET_ZOOM)) {
			resetZoom();
		} else if(display.isControlActivated(Control.INCREASEZOOM)) {
			computeZoom(zoomSpeed, nanoseconds);
		} else if(display.isControlActivated(Control.DECREASEZOOM)) {
			computeZoom(-zoomSpeed, nanoseconds);
		}
		
		float posX = target.getPositionX(), posY = target.getPositionY();
		camera.setPositionX(posX);
		camera.setPositionY(posY);
		audio.setPosition(posX, posY);
	}
	
	private void computeZoom(float zoomSpeed, long nanoseconds) {
		camera.setZoom((Math.max(Math.min(maxZoom, camera.getZoom() + camera.getZoom() * nanoseconds * zoomSpeed / 1_000_000_000f), minZoom)));
	}
	
	public void resetZoom() {
		camera.setZoom(minZoom + maxZoom * 0.1f);
	}

	@Override
	public void mouseScroll(double xoffset, double yoffset) {
		camera.setZoom((Math.max(Math.min(maxZoom, camera.getZoom() + camera.getZoom() * (float)yoffset * 0.2f), minZoom)));
	}

	public Camera getCamera() {
		return camera;
	}
}
