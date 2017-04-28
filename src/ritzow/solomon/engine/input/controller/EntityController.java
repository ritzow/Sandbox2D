package ritzow.solomon.engine.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static ritzow.solomon.engine.util.Utility.intersection;

import ritzow.solomon.engine.input.Controls;
import ritzow.solomon.engine.input.InputManager;
import ritzow.solomon.engine.input.handler.KeyHandler;
import ritzow.solomon.engine.world.base.DefaultWorld;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.entity.Entity;

public final class EntityController implements Controller, KeyHandler {
	private final World world;
	private final Entity entity;
	private final float movementSpeed;
	private final boolean allowFlight;
	
	private boolean left;
	private boolean right;
	private boolean up;
	
	public EntityController(Entity entity, World world, float movementSpeed, boolean allowFlight) {
		this.world = world;
		this.entity = entity;
		this.movementSpeed = movementSpeed;
		this.allowFlight = allowFlight;
	}
	
	@Override
	public void update() {
		if(left) {
			if(!blockLeft()) {
				entity.setVelocityX(-movementSpeed);
			}
			
			else if(!blockLeft(entity.getPositionX(), entity.getPositionY() + getJumpHeight(((DefaultWorld)world).getGravity()), entity.getWidth(), entity.getHeight()) && canJump()) {
				entity.setVelocityY(movementSpeed);
			}
		} else if(right) {
			if(!blockRight()) {
				entity.setVelocityX(movementSpeed);
			}
			
			else if(!blockRight(entity.getPositionX(), entity.getPositionY() + getJumpHeight(((DefaultWorld)world).getGravity()), entity.getWidth(), entity.getHeight()) && canJump()) {
				entity.setVelocityY(movementSpeed);
			}
		}
		
		if(up && (allowFlight || canJump())) {
			entity.setVelocityY(movementSpeed);
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
		} else if(key == Controls.KEYBIND_LEFT) {
			if(action == GLFW_PRESS) {
				left = true;
			}
			
			else if(action == GLFW_RELEASE) {
				left = false;
			}
		} else if(key == Controls.KEYBIND_RIGHT) {
			if(action == GLFW_PRESS) {
				right = true;
			}
			
			else if(action == GLFW_RELEASE) {
				right = false;
			}
		}
	}
	
	private float getJumpHeight(float gravity) {
		return (0 - (float)Math.pow(movementSpeed, 2))/(2 * -gravity);
	}
	
	private boolean canJump() {
		return entity.getVelocityY() <= 0 && (entityBelow() || blockBelow());
	}
	
	private boolean blockBelow() {
		return blockBelow(entity.getPositionX(), entity.getPositionY(), entity.getWidth(), entity.getHeight());
	}
	
	private boolean blockBelow(float x, float y, float width, float height) {
		for(float h = x - width/2; h <= x + width/2; h++) {	
			if(world.getForeground().isBlock(h + (h < x ? +0.05f : -0.05f), y - height/2 - 0.05f)
					&& world.getForeground().get(h + (h < x ? +0.05f : -0.05f), y - height/2 - 0.05f).isSolid()) {
				return true;
			}
		}
		return false;
	}
	
	private boolean blockLeft() {
		return blockLeft(entity.getPositionX(), entity.getPositionY(), entity.getWidth(), entity.getHeight());
	}
	
	private boolean blockLeft(float x, float y, float width, float height) {
		for(float v = y - height/2; v <= y + height/2; v++) {
			if(world.getForeground().isBlock(x - width/2 - 0.05f, v + (v < y ? +0.05f : -0.05f)) 
					&& world.getForeground().get(x - width/2 - 0.05f, v + (v < y ? +0.05f : -0.05f)).isSolid()) {
				return true;
			}
		}
		return false;
	}
	
	private boolean blockRight() {
		return blockRight(entity.getPositionX(), entity.getPositionY(), entity.getWidth(), entity.getHeight());
	}
	
	private boolean blockRight(float x, float y, float width, float height) {
		for(float v = y - height/2; v <= y + height/2; v++) {
			if(world.getForeground().isBlock(x + width/2 + 0.05f, v + (v < y ? +0.05f : -0.05f)) && world.getForeground()
					.get(x + width/2 + 0.05f, v + (v < y ? +0.05f : -0.05f)).isSolid()) {
				return true;
			}
		}
		return false;
	}
	
	private boolean entityBelow() {
		synchronized(world) {
			for(Entity o : world) {
				if(o != null && entity != o && o.doEntityCollisionResolution() && 
						intersection(entity.getPositionX(), entity.getPositionY() - entity.getHeight()/2, entity.getWidth() - 0.01f, 0.1f, 
								o.getPositionX(), o.getPositionY(), o.getWidth(), o.getHeight())) {
					return true;
				}
			}
			return false;
		}
	}
	
	@Override
	public void link(InputManager input) {
		input.getKeyHandlers().add(this);
	}

	@Override
	public void unlink(InputManager input) {
		input.getKeyHandlers().remove(this);
	}
}
