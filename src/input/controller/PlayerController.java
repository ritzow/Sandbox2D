package input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import input.Controls;
import input.InputManager;
import input.handler.KeyHandler;
import main.Updatable;
import world.World;
import world.entity.GenericEntity;

public final class PlayerController implements KeyHandler, Updatable {
	
	private World world;
	private GenericEntity player;
	private float movementSpeed;
	
	private boolean upPressed;
	private boolean rightPressed;
	private boolean downPressed;
	private boolean leftPressed;
	
	public PlayerController(InputManager manager, GenericEntity player, World world, float movementSpeed) {
		this.world = world;
		this.player = player;
		this.movementSpeed = movementSpeed;
		manager.getKeyHandlers().add(this);
	}
	
	public void update() {
		if(upPressed && (world.entityBelow(player) || world.blockBelow(player))) {
			player.velocity().setY(movementSpeed * 1.5f); //TODO check above to make sure player doesn't jump when ceiling is present
		}
		
		if(downPressed) {
			player.getHitbox().setHeight(0.75f);
			player.getGraphics().scale().setY(0.75f);
		} else {
			player.getHitbox().setHeight(1);
			player.getGraphics().scale().setY(1);
		}
		
		if(rightPressed && !world.blockRight(player)) {
			player.velocity().setX(movementSpeed);
		} else if(leftPressed && !world.blockLeft(player)) {
			player.velocity().setX(-movementSpeed);
		}
		
		if (!leftPressed && !rightPressed) {
			player.velocity().setX(0);
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
