package input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import input.Controls;
import input.InputManager;
import input.handler.KeyHandler;
import util.Updatable;
import world.World;
import world.entity.Entity;

public final class EntityController extends Controller implements KeyHandler, Updatable {
	
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
		this.friction = entity.getFriction();
	}
	
	@Override
	public void update() {

		if(left) {
			if(!world.blockLeft(entity)) {
				entity.setVelocityX(-movementSpeed);
			}
			
			else if(entity.getVelocityY() <= 0 && !world.getForeground().isBlock(entity.getPositionX() - 1, entity.getPositionY() + 1) && canJump()) {
				entity.setVelocityY(movementSpeed);
			}
		}
		
		if(right) {
			if(!world.blockRight(entity)) {
				entity.setVelocityX(movementSpeed);
			}
			
			else if(entity.getVelocityY() <= 0 && !world.getForeground().isBlock(entity.getPositionX() + 1, entity.getPositionY() + 1) && canJump()) {
				entity.setVelocityY(movementSpeed);
			}
		}
		
		if(up && canJump()) {
			entity.setVelocityY(movementSpeed);
		}
		
		if(down) {
			entity.setFriction(1);
		}
		
		else {
			entity.setFriction(friction);
		}
	}
	
	private boolean canJump() {
		return entity.getVelocityY() <= 0 && (world.entityBelow(entity) || world.blockBelow(entity));
	}
	
	@Override
	public void link(InputManager input) {
		input.getKeyHandlers().add(this);
	}

	@Override
	public void unlink(InputManager input) {
		input.getKeyHandlers().remove(this);
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
