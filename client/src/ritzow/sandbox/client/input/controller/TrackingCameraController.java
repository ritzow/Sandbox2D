package ritzow.sandbox.client.input.controller;

import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.entity.Entity;

public final class TrackingCameraController {
	private final Camera camera;
	private final float zoomSpeedNanos, minZoom, maxZoom;

	public TrackingCameraController(float zoomSpeed, float minZoom, float maxZoom) {
		this.camera = new Camera(0, 0, minZoom);
		this.zoomSpeedNanos = Utility.convertPerSecondToPerNano(zoomSpeed);
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
	}

	public void update(Display display, Entity target, AudioSystem audio, long nanoseconds) {
		if(display.isControlActivated(Control.RESET_ZOOM)) {
			resetZoom();
		} else if(display.isControlActivated(Control.INCREASEZOOM)) {
			computeZoom(zoomSpeedNanos, nanoseconds);
		} else if(display.isControlActivated(Control.DECREASEZOOM)) {
			computeZoom(-zoomSpeedNanos, nanoseconds);
		}
		
		float posX = target.getPositionX();
		float posY = target.getPositionY();
		camera.setPositionX(posX);
		camera.setPositionY(posY);
		audio.setPosition(posX, posY);
	}
	
	public void resetZoom() {
		camera.setZoom(Math.fma(0.1f, maxZoom, minZoom));
	}

	@SuppressWarnings("unused")
	public void mouseScroll(double xoffset, double yoffset) {
		computeZoom(0.2f, (float)yoffset);
	}
	
	private void computeZoom(float scale, float offset) {
		camera.setZoom(Utility.clamp(minZoom, camera.getZoom() * Math.fma(scale, offset, 1.0f), maxZoom));
	}

	public Camera getCamera() {
		return camera;
	}
}
