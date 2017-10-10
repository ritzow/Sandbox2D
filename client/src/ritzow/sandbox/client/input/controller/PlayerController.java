package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import ritzow.sandbox.client.Client;
import ritzow.sandbox.client.Client.PlayerAction;
import ritzow.sandbox.client.input.Controls;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.world.entity.Entity;

public class PlayerController implements Controller, KeyHandler {
	protected final Client client;
	
	public PlayerController(Client client) {
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
				client.getPlayer().setUp(true);
				client.sendPlayerAction(PlayerAction.MOVE_UP, true);
			} else if(action == GLFW_RELEASE) {
				client.getPlayer().setUp(false);
				client.sendPlayerAction(PlayerAction.MOVE_UP, false);
			}
			break;
		case Controls.KEYBIND_RIGHT:
			if(action == GLFW_PRESS) {
				client.getPlayer().setRight(true);
				client.sendPlayerAction(PlayerAction.MOVE_RIGHT, true);
			} else if(action == GLFW_RELEASE) {
				client.getPlayer().setRight(false);
				client.sendPlayerAction(PlayerAction.MOVE_RIGHT, false);
			}
			break;
		case Controls.KEYBIND_LEFT:
			if(action == GLFW_PRESS) {
				client.getPlayer().setLeft(true);
				client.sendPlayerAction(PlayerAction.MOVE_LEFT, true);
			} else if(action == GLFW_RELEASE) {
				client.getPlayer().setLeft(false);
				client.sendPlayerAction(PlayerAction.MOVE_LEFT, false);
			}
			break;
		case Controls.KEYBIND_DOWN:
			if(action == GLFW_PRESS) {
				client.getPlayer().setDown(true);
				client.sendPlayerAction(PlayerAction.MOVE_DOWN, true);
			} else if(action == GLFW_RELEASE) {
				client.getPlayer().setDown(false);
				client.sendPlayerAction(PlayerAction.MOVE_DOWN, false);
			}
			break;
		}
	}
	
	private boolean canJump() {
		return client.getPlayer().getVelocityY() <= 0 && (entityBelow() || blockBelow());
	}
	
	private boolean blockBelow() {
		return blockBelow(client.getPlayer().getPositionX(), client.getPlayer().getPositionY(), client.getPlayer().getWidth(), client.getPlayer().getHeight());
	}
	
	private boolean blockBelow(float x, float y, float width, float height) {
		for(float h = x - width/2; h <= x + width/2; h++) {	
			if(client.getWorld().getForeground().isBlock(h + (h < x ? +0.05f : -0.05f), y - height/2 - 0.05f)
					&& client.getWorld().getForeground().get(h + (h < x ? +0.05f : -0.05f), y - height/2 - 0.05f).isSolid()) {
				return true;
			}
		}
		return false;
	}

	private boolean entityBelow() {
		for(Entity e : client.getWorld().getEntitiesInRectangle(client.getPlayer().getPositionX(), client.getPlayer().getPositionY(), client.getPlayer().getWidth(), client.getPlayer().getHeight())) {
			if(e.doEntityCollisionResolution()) {
				return true;
			}
		}
		return false;
//		for(Entity o : world) {
//			if(o != null && player != o && o.doEntityCollisionResolution() && 
//					intersection(client.getPlayer().getPositionX(), client.getPlayer().getPositionY() - client.getPlayer().getHeight()/2, client.getPlayer().getWidth() - 0.01f, 0.1f, 
//							o.getPositionX(), o.getPositionY(), o.getWidth(), o.getHeight())) {
//				return true;
//			}
//		}
	}
}
