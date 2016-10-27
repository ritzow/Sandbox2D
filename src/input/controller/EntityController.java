package input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static util.HitboxUtil.intersection;

import input.Controls;
import input.InputManager;
import input.handler.KeyHandler;
import util.Updatable;
import world.World;
import world.block.RedBlock;
import world.entity.Entity;
import world.entity.ItemEntity;
import world.item.BlockItem;

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
			if(!blockLeft()) {
				entity.setVelocityX(-movementSpeed);
			}
			
			else if(!blockLeft(entity.getPositionX(), entity.getPositionY() + getJumpHeight(world.getGravity()), entity.getWidth(), entity.getHeight()) && canJump()) {
				entity.setVelocityY(movementSpeed);
			}
		}
		
		if(right) {
			if(!blockRight()) {
				entity.setVelocityX(movementSpeed);
			}
			
			else if(!blockRight(entity.getPositionX(), entity.getPositionY() + getJumpHeight(world.getGravity()), entity.getWidth(), entity.getHeight()) && canJump()) {
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
			if(world.getForeground().isBlock(h + (h < x ? +0.05f : -0.05f), y - height/2 - 0.05f)) {
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
			if(world.getForeground().isBlock(x - width/2 - 0.05f, v + (v < y ? +0.05f : -0.05f))) {
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
			if(world.getForeground().isBlock(x + width/2 + 0.05f, v + (v < y ? +0.05f : -0.05f))) {
				return true;
			}
		}
		return false;
	}
	
	private boolean entityBelow() {
		for(int i = 0; i < world.getEntities().size(); i++) {
			Entity o = world.getEntities().get(i);
			if(o != null && entity != o && o.getDoEntityCollisionResolution() && 
					intersection(entity.getPositionX(), entity.getPositionY() - entity.getHeight()/2, entity.getWidth() - 0.01f, 0.1f, 
							o.getPositionX(), o.getPositionY(), o.getWidth(), o.getHeight())) {
				return true;
			}
		}
		return false;
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
				world.getEntities().add(new ItemEntity(new BlockItem(new RedBlock()), entity.getPositionX(), entity.getPositionY()));
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
