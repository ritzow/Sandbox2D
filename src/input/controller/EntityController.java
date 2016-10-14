package input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import input.Controls;
import input.handler.KeyHandler;
import main.Updatable;
import world.World;
import world.entity.Entity;

public final class EntityController implements KeyHandler, Updatable {
	
	private World world;
	private Entity entity;
	private float movementSpeed;
	
	private boolean upPressed;
	private boolean rightPressed;
	private boolean downPressed;
	private boolean leftPressed;
	
	public EntityController(Entity entity, World world, float movementSpeed) {
		this.world = world;
		this.entity = entity;
		this.movementSpeed = movementSpeed;
	}
	
	public void update() {
		
		if(leftPressed && !world.blockLeft(entity)) {
			entity.velocity().addX(-movementSpeed);
		}
		
		else {
			
		}
		
		if(rightPressed && !world.blockRight(entity)) {
			entity.velocity().addX(movementSpeed);
		}
		
		else {
			
		}
		
		if(upPressed && (world.blockBelow(entity) || world.entityBelow(entity))) {
			entity.velocity().setAccelerationY(world.getGravity() * 20);
		}
		
		if(downPressed) {
			
		}
	}

	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) {
		if(key == Controls.KEYBIND_UP) {
			if(action == GLFW_PRESS) {
				upPressed = true;
			}
			
			else if(action == GLFW_RELEASE) {
				upPressed = false;
			}
		}
		
		else if(key == Controls.KEYBIND_DOWN) {
			if(action == GLFW_PRESS) {
				downPressed = true;
			}
			
			else if(action == GLFW_RELEASE) {
				downPressed = false;
			}
		}
		
		else if(key == Controls.KEYBIND_LEFT) {
			if(action == GLFW_PRESS) {
				leftPressed = true;
			}
			
			else if(action == GLFW_RELEASE) {
				leftPressed = false;
			}
		}
		
		else if(key == Controls.KEYBIND_RIGHT) {
			if(action == GLFW_PRESS) {
				rightPressed = true;
			}
			
			else if(action == GLFW_RELEASE) {
				rightPressed = false;
			}
		}
	}
}
