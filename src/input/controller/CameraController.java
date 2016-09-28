package input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import input.Controls;
import input.InputManager;
import input.handler.KeyHandler;
import input.handler.MouseButtonHandler;
import input.handler.ScrollHandler;
import main.Updatable;
import world.Camera;
import world.entity.Entity;

public class CameraController implements KeyHandler, ScrollHandler, MouseButtonHandler, Updatable {
	
	protected Camera camera;
	protected float zoomSpeed;
	
	protected Entity target;
	
	public CameraController(InputManager manager, Camera camera, Entity target, float zoomSpeed) {
		this.camera = camera;
		this.zoomSpeed = zoomSpeed;
		this.target = target;
		manager.getKeyHandlers().add(this);
		manager.getScrollHandlers().add(this);
		manager.getMouseButtonHandlers().add(this);
	}
	
	public void update() {
		camera.setX(target.position().getX());
		camera.setY(target.position().getY());
		camera.update();
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
