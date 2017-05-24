package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static ritzow.sandbox.util.Utility.intersection;

import ritzow.sandbox.client.Client;
import ritzow.sandbox.client.Client.PlayerAction;
import ritzow.sandbox.client.input.Controls;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

public class PlayerController implements Controller, KeyHandler {
	protected final PlayerEntity player;
	protected final World world;
	protected final Client client;
	
	public PlayerController(PlayerEntity player, World world, Client client) {
		this.player = player;
		this.world = world;
		this.client = client;
	}

	@Override
	public void link(InputManager manager) {
		manager.getKeyHandlers().add(this);
	}

	@Override
	public void unlink(InputManager manager) {
		manager.getKeyHandlers().remove(this);
	}

	@Override
	public void update() {
		
	}

	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) {
		
		switch(key) {
		case Controls.KEYBIND_UP:
			if(action == GLFW_PRESS && canJump()) {
				player.setUp(true);
				client.sendPlayerAction(PlayerAction.MOVE_UP, true);
			} else if(action == GLFW_RELEASE) {
				player.setUp(false);
				client.sendPlayerAction(PlayerAction.MOVE_UP, false);
			}
			break;
		case Controls.KEYBIND_RIGHT:
			if(action == GLFW_PRESS) {
				player.setRight(true);
				client.sendPlayerAction(PlayerAction.MOVE_RIGHT, true);
			} else if(action == GLFW_RELEASE) {
				player.setRight(false);
				client.sendPlayerAction(PlayerAction.MOVE_RIGHT, false);
			}
			break;
		case Controls.KEYBIND_LEFT:
			if(action == GLFW_PRESS) {
				player.setLeft(true);
				client.sendPlayerAction(PlayerAction.MOVE_LEFT, true);
			} else if(action == GLFW_RELEASE) {
				player.setLeft(false);
				client.sendPlayerAction(PlayerAction.MOVE_LEFT, false);
			}
			break;
		case Controls.KEYBIND_DOWN:
			if(action == GLFW_PRESS) {
				player.setDown(true);
				client.sendPlayerAction(PlayerAction.MOVE_DOWN, true);
			} else if(action == GLFW_RELEASE) {
				player.setDown(false);
				client.sendPlayerAction(PlayerAction.MOVE_DOWN, false);
			}
			break;
		}
	}
	
	private boolean canJump() {
		return player.getVelocityY() <= 0 && (entityBelow() || blockBelow());
	}
	
	private boolean blockBelow() {
		return blockBelow(player.getPositionX(), player.getPositionY(), player.getWidth(), player.getHeight());
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
	
	private boolean entityBelow() {
		synchronized(world) {
			for(Entity o : world) {
				if(o != null && player != o && o.doEntityCollisionResolution() && 
						intersection(player.getPositionX(), player.getPositionY() - player.getHeight()/2, player.getWidth() - 0.01f, 0.1f, 
								o.getPositionX(), o.getPositionY(), o.getWidth(), o.getHeight())) {
					return true;
				}
			}
			return false;
		}
	}
}
