package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.world.entity.Entity;

public final class TrackingCameraController extends CameraController {
	private final Entity target;
	private final AudioSystem audio;
	private final float zoomSpeed, minZoom, maxZoom;
	private float velocityZ;

	public TrackingCameraController(Camera camera, AudioSystem audio, Entity target, float zoomSpeed, float minZoom, float maxZoom) {
		super(camera);
		this.audio = audio;
		this.target = target;
		this.zoomSpeed = zoomSpeed;
		this.minZoom = minZoom / target.getWidth();
		this.maxZoom = maxZoom / target.getWidth();
		this.camera.setZoom(minZoom);
	}

	public void update(long nanoseconds) {
		camera.setPositionX(target.getPositionX());
		camera.setPositionY(target.getPositionY());
		camera.setZoom((Math.max(Math.min(maxZoom, camera.getZoom() + camera.getZoom() * nanoseconds * velocityZ / 1_000_000_000f), minZoom)));
		audio.setPosition(camera.getPositionX(), camera.getPositionY());
	}

	@Override
	public Camera getCamera() {
		return camera;
	}

	@Override
	public void mouseScroll(double xoffset, double yoffset) {
		camera.setZoom((Math.max(Math.min(maxZoom, camera.getZoom() + camera.getZoom() * (float)yoffset * 0.2f), minZoom)));
	}

	@Override
	public void mouseButton(int button, int action, int mods) {
		if(button == GLFW_MOUSE_BUTTON_MIDDLE && action == GLFW_PRESS) {
			camera.setZoom(minZoom + maxZoom * 0.1f);
		}
	}

	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) {
		if(key == ControlScheme.KEYBIND_INCREASEZOOM) {
			if(action == GLFW_PRESS) {
				velocityZ = zoomSpeed;
			} else if(action == GLFW_RELEASE) {
				velocityZ = 0;
			}
		} else if(key == ControlScheme.KEYBIND_DECREASEZOOM) {
			if(action == GLFW_PRESS) {
				velocityZ = -zoomSpeed;
			} else if(action == GLFW_RELEASE) {
				velocityZ = 0;
			}
		}
	}
}
