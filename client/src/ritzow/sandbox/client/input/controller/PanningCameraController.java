package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.ControlScheme;

public final class PanningCameraController extends CameraController {
	private final AudioSystem audio;
	private final float panSpeed, zoomSpeed, minZoom, maxZoom;
	private float velocityX, velocityY, velocityZ;

	public PanningCameraController(Camera camera, AudioSystem audio, float panSpeed, float minZoom, float maxZoom) {
		super(camera);
		this.audio = audio;
		this.panSpeed = panSpeed;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
		this.zoomSpeed = 0.2f;
	}

	public void update() {
		camera.setPositionX(camera.getPositionX() + velocityX);
		camera.setPositionY(camera.getPositionY() + velocityY);
		camera.setZoom((Math.max(Math.min(maxZoom, camera.getZoom() + camera.getZoom() * velocityZ), minZoom)));
		audio.setPosition(camera.getPositionX(), camera.getPositionY());
	}

	@Override
	public void mouseButton(int button, int action, int mods) {
		if(button == GLFW_MOUSE_BUTTON_MIDDLE && action == GLFW_PRESS) {
			camera.setZoom(minZoom + maxZoom * 0.1f);
		}
	}

	@Override
	public void mouseScroll(double xoffset, double yoffset) {
		camera.setZoom((Math.max(Math.min(maxZoom, camera.getZoom() + camera.getZoom() * (float)yoffset * 0.2f), minZoom)));
	}

	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) {
		switch(key) {
		case ControlScheme.KEYBIND_INCREASEZOOM:
			if(action == GLFW_PRESS) {
				velocityZ += zoomSpeed;
			} else if(action == GLFW_RELEASE) {
				velocityZ = 0;
			} break;
		case ControlScheme.KEYBIND_DECREASEZOOM:
			if(action == GLFW_PRESS) {
				velocityZ -= zoomSpeed;
			} else if(action == GLFW_RELEASE) {
				velocityZ = 0;
			} break;
		case ControlScheme.KEYBIND_LEFT:
			if(action == GLFW_PRESS) {
				velocityX -= panSpeed;
			} else if(action == GLFW_RELEASE) {
				velocityX = 0;
			} break;
		case ControlScheme.KEYBIND_RIGHT:
			if(action == GLFW_PRESS) {
				velocityX += panSpeed;
			} else if(action == GLFW_RELEASE) {
				velocityX = 0;
			} break;
		case ControlScheme.KEYBIND_UP:
			if(action == GLFW_PRESS) {
				velocityY += panSpeed;
			} else if(action == GLFW_RELEASE) {
				velocityY = 0;
			} break;
		case ControlScheme.KEYBIND_DOWN:
			if(action == GLFW_PRESS) {
				velocityY -= panSpeed;
			} else if(action == GLFW_RELEASE) {
				velocityY = 0;
			} break;
		}
	}
}
