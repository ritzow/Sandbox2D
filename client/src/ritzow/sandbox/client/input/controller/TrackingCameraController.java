package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import ritzow.sandbox.client.audio.ClientAudioSystem;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.Controls;
import ritzow.sandbox.world.entity.Entity;

public final class TrackingCameraController extends CameraController {
	protected final Entity target;
	protected final ClientAudioSystem audio;
	protected float zoomSpeed;
	protected final float soundFalloff;
	protected float velocityZ;
	protected final float minZoom;
	protected final float maxZoom;
	protected long previousTime;
	
	public TrackingCameraController(Camera camera, ClientAudioSystem audio, Entity target, float zoomSpeed, float minZoom, float maxZoom) {
		super(camera);
		this.zoomSpeed = zoomSpeed;
		this.target = target;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
		this.soundFalloff = 1.5f;
		this.camera.setZoom(minZoom);
		this.audio = audio;
	}
	
	@Override
	public void update() {
		if(previousTime == 0)
			previousTime = System.nanoTime();
		camera.setPositionX(target.getPositionX());
		camera.setPositionY(target.getPositionY());
		long time = System.nanoTime();
		camera.setZoom((Math.max(Math.min(maxZoom, camera.getZoom() + camera.getZoom() * (time - previousTime)/1000000 * velocityZ), minZoom)));
		previousTime = time;
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
		if(key == Controls.KEYBIND_INCREASEZOOM) {
			if(action == GLFW_PRESS) {
				velocityZ = zoomSpeed;
			}
			
			else if(action == GLFW_RELEASE) {
				velocityZ = 0;
			}
		}
		
		else if(key == Controls.KEYBIND_DECREASEZOOM) {
			if(action == GLFW_PRESS) {
				velocityZ = -zoomSpeed;
			}
			
			else if(action == GLFW_RELEASE) {
				velocityZ = 0;
			}
		}
	}
}
