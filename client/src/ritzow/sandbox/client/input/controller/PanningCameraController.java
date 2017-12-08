package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import ritzow.sandbox.client.audio.ClientAudioSystem;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.Controls;

public final class PanningCameraController extends CameraController {
	protected final ClientAudioSystem audio;
	protected final float panSpeed;
	protected final float zoomSpeed;
	protected final float minZoom;
	protected final float maxZoom;
	
	protected float velocityX;
	protected float velocityY;
	protected float velocityZ;
	
	protected boolean up;
	protected boolean down;
	protected boolean left;
	protected boolean right;
	
	public PanningCameraController(Camera camera, ClientAudioSystem audio, float panSpeed, float minZoom, float maxZoom) {
		super(camera);
		this.audio = audio;
		this.panSpeed = panSpeed;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
		this.zoomSpeed = 0.2f;
	}
	
	@Override
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
		
		else if(key == Controls.KEYBIND_LEFT) {
			if(action == GLFW_PRESS) {
				left = true;
				velocityX -= panSpeed;
			}
			
			else if(action == GLFW_RELEASE) {
				left = false;
				velocityX = 0;
			}
		}
		
		else if(key == Controls.KEYBIND_RIGHT) {
			if(action == GLFW_PRESS) {
				right = true;
				velocityX += panSpeed;
			}
			
			else if(action == GLFW_RELEASE) {
				right = false;
				velocityX = 0;
			}
		}
		
		else if(key == Controls.KEYBIND_UP) {
			if(action == GLFW_PRESS) {
				up = true;
				velocityY += panSpeed;
			}
			
			else if(action == GLFW_RELEASE) {
				up = false;
				velocityY = 0;
			}
		}
		
		else if(key == Controls.KEYBIND_DOWN) {
			if(action == GLFW_PRESS) {
				down = true;
				velocityY -= panSpeed;
			}
			
			else if(action == GLFW_RELEASE) {
				down = false;
				velocityY = 0;
			}
		}
	}
	
}
