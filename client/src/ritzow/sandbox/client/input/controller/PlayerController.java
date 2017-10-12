package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;

import ritzow.sandbox.client.Client;
import ritzow.sandbox.client.input.Controls;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.world.entity.PlayerEntity;

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
		if(action == GLFW_REPEAT)
			return;
		boolean enabled = action == GLFW_PRESS;
		PlayerEntity player = client.getPlayer();
		PlayerAction playerAction;
		
		switch(key) {
		case Controls.KEYBIND_UP:
			playerAction = PlayerAction.MOVE_UP; break;
		case Controls.KEYBIND_RIGHT:
			playerAction = PlayerAction.MOVE_RIGHT; break;
		case Controls.KEYBIND_LEFT:
			playerAction = PlayerAction.MOVE_LEFT; break;
		case Controls.KEYBIND_DOWN:
			playerAction = PlayerAction.MOVE_DOWN; break;
		default:
			return;
		}
		
		client.sendPlayerAction(playerAction, enabled);
		player.processAction(playerAction, enabled);
	}
	
//	TODO fix, not working
//	private boolean canJump() {
//		return client.getPlayer().getVelocityY() <= 0 && (entityBelow() || blockBelow());
//	}
//	
//	private boolean blockBelow() {
//		return blockBelow(client.getPlayer().getPositionX(), client.getPlayer().getPositionY(), client.getPlayer().getWidth(), client.getPlayer().getHeight());
//	}
//	
//	private boolean blockBelow(float x, float y, float width, float height) {
//		for(float h = x - width/2; h <= x + width/2; h++) {	
//			if(client.getWorld().getForeground().isBlock(h + (h < x ? +0.05f : -0.05f), y - height/2 - 0.05f)
//					&& client.getWorld().getForeground().get(h + (h < x ? +0.05f : -0.05f), y - height/2 - 0.05f).isSolid()) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	private boolean entityBelow() {
//		for(Entity e : client.getWorld().getEntitiesInRectangle(client.getPlayer().getPositionX(), client.getPlayer().getPositionY(), client.getPlayer().getWidth(), client.getPlayer().getHeight())) {
//			if(e.doEntityCollisionResolution()) {
//				return true;
//			}
//		}
//		return false;
//	}
}
