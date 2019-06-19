package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.world.entity.Entity;

public final class TrackingCameraController extends CameraController {
	private final float zoomSpeed, minZoom, maxZoom;
	private float velocityZ;

	public TrackingCameraController(float zoomSpeed, float minZoom, float maxZoom) {
		super(new Camera(0, 0, 1));
		this.zoomSpeed = zoomSpeed;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
		this.camera.setZoom(minZoom);
	}

	public void update(Entity target, AudioSystem audio, long nanoseconds) {
		camera.setPositionX(target.getPositionX());
		camera.setPositionY(target.getPositionY());
		camera.setZoom((Math.max(Math.min(maxZoom, camera.getZoom() + camera.getZoom() * nanoseconds * velocityZ / 1_000_000_000f), minZoom)));
		audio.setPosition(camera.getPositionX(), camera.getPositionY());
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
		switch(key) {
			case ControlScheme.KEYBIND_INCREASEZOOM -> {
				if(action == GLFW_PRESS) {
					velocityZ = zoomSpeed;
				} else if(action == GLFW_RELEASE) {
					velocityZ = 0;
				}
			}

			case ControlScheme.KEYBIND_DECREASEZOOM -> {
				if(action == GLFW_PRESS) {
					velocityZ = -zoomSpeed;
				} else if(action == GLFW_RELEASE) {
					velocityZ = 0;
				}
			}
		}
	}
}
