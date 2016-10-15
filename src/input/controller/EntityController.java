package input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import input.Controls;
import input.handler.KeyHandler;
import util.Updatable;
import world.World;
import world.entity.Entity;

public final class EntityController implements KeyHandler, Updatable {
	
	private World world;
	private Entity entity;
	private float movementSpeed;
	
	private boolean left;
	private boolean right;
	private boolean up;
	private boolean down;
	
	private float friction;
	
	public EntityController(Entity entity, World world, float movementSpeed) {
		this.world = world;
		this.entity = entity;
		this.movementSpeed = movementSpeed;
		this.friction = entity.getHitbox().getFriction();
	}
	
	@Override
	public void update() {

		if(left && !(world.blockLeft(entity))) {
			entity.velocity().setX(-movementSpeed);
		}
		
		if(right && !(world.blockRight(entity))) {
			entity.velocity().setX(movementSpeed);
		}
		
		if(up && (world.entityBelow(entity) || world.blockBelow(entity))) {
			entity.velocity().setY(movementSpeed);
		}
		
		if(down) {
			entity.getHitbox().setFriction(1);
		}
		
		else {
			entity.getHitbox().setFriction(friction);
		}
	}

	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) {
		if(key == Controls.KEYBIND_UP) {
			if(action == GLFW_PRESS) {
				up = true;
			}
			
			else if(action == GLFW_RELEASE) {
				up = false;
			}
		}
		
		else if(key == Controls.KEYBIND_DOWN) {
			if(action == GLFW_PRESS) {
				down = true;
			}
			
			else if(action == GLFW_RELEASE) {
				down = false;
			}
		}
		
		else if(key == Controls.KEYBIND_LEFT) {
			if(action == GLFW_PRESS) {
				left = true;
			}
			
			else if(action == GLFW_RELEASE) {
				left = false;
			}
		}
		
		else if(key == Controls.KEYBIND_RIGHT) {
			if(action == GLFW_PRESS) {
				right = true;
			}
			
			else if(action == GLFW_RELEASE) {
				right = false;
			}
		}
	}
}
