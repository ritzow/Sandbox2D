package input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import audio.AudioSystem;
import graphics.Camera;
import input.Controls;
import world.entity.Entity;

public class TrackingCameraController extends CameraController {
	protected Camera camera;
	protected Entity target;
	protected float zoomSpeed;
	protected float soundFalloff;
	protected float velocityZ;
	protected float minZoom;
	protected float maxZoom;
	
	public TrackingCameraController(Camera camera, Entity target, float zoomSpeed, float minZoom, float maxZoom) {
		this.camera = camera;
		this.zoomSpeed = zoomSpeed;
		this.target = target;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
		this.soundFalloff = 1.5f;
		this.camera.setZoom(minZoom);
	}
	
	public void update() {
		camera.setPositionX(target.getPositionX());
		camera.setPositionY(target.getPositionY());
		camera.setZoom((Math.max(Math.min(maxZoom, camera.getZoom() + camera.getZoom() * velocityZ), minZoom)));
		AudioSystem.setListenerProperties(camera.getPositionX(), camera.getPositionY(), -(maxZoom - camera.getZoom()) * soundFalloff, target.getVelocityX(), 
				target.getVelocityY(), target.getVelocityX(), target.getVelocityY());
	}
	
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
				velocityZ += zoomSpeed;
			}
			
			else if(action == GLFW_RELEASE) {
				velocityZ = 0;
			}
		}
		
		else if(key == Controls.KEYBIND_DECREASEZOOM) {
			if(action == GLFW_PRESS) {
				velocityZ -= zoomSpeed;
			}
			
			else if(action == GLFW_RELEASE) {
				velocityZ = 0;
			}
		}
	}
}
