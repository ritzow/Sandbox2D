package input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import static org.lwjgl.openal.AL10.*;

import graphics.Camera;
import input.Controls;
import input.InputManager;
import input.handler.KeyHandler;
import input.handler.MouseButtonHandler;
import input.handler.ScrollHandler;
import util.Updatable;
import world.entity.Entity;

public class CameraController extends Controller implements KeyHandler, ScrollHandler, MouseButtonHandler, Updatable {
	
	protected Camera camera;
	protected float zoomSpeed;
	
	protected Entity target;
	
	public CameraController(Camera camera, Entity target, float zoomSpeed) {
		this.camera = camera;
		this.zoomSpeed = zoomSpeed;
		this.target = target;
	}
	
	public void update() {
		camera.setX(target.getPositionX());
		camera.setY(target.getPositionY());
		camera.update();
		alListener3f(AL_POSITION, camera.getX(), camera.getY(), 0);
		alListenerf(AL_GAIN, camera.getZoom() * 5);
	}
	
	@Override
	public void link(InputManager input) {
		input.getKeyHandlers().add(this);
		input.getScrollHandlers().add(this);
		input.getMouseButtonHandlers().add(this);
	}

	@Override
	public void unlink(InputManager input) {
		input.getKeyHandlers().remove(this);
		input.getScrollHandlers().remove(this);
		input.getMouseButtonHandlers().remove(this);
	}

	@Override
	public void mouseScroll(double xoffset, double yoffset) {
		camera.setZoom(camera.getZoom() + camera.getZoom() * (float)yoffset * 0.2f);
	}
	
	@Override
	public void mouseButton(int button, int action, int mods) {
		if(button == GLFW_MOUSE_BUTTON_MIDDLE && action == GLFW_PRESS) {
			camera.resetZoom();
		}
	}

	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) {
		if(key == Controls.KEYBIND_INCREASEZOOM) {
			if(action == GLFW_PRESS) {
				camera.setVelocityZ(camera.getVelocityZ() + zoomSpeed);
			}
			
			else if(action == GLFW_RELEASE) {
				camera.setVelocityZ(0);
			}
		}
		
		else if(key == Controls.KEYBIND_DECREASEZOOM) {
			if(action == GLFW_PRESS) {
				camera.setVelocityZ(camera.getVelocityZ() - zoomSpeed);
			}
			
			else if(action == GLFW_RELEASE) {
				camera.setVelocityZ(0);
			}
		}
	}
}
