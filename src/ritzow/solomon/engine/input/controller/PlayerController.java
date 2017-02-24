package ritzow.solomon.engine.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static ritzow.solomon.engine.util.Utility.intersection;

import ritzow.solomon.engine.input.Controls;
import ritzow.solomon.engine.input.InputManager;
import ritzow.solomon.engine.input.handler.KeyHandler;
import ritzow.solomon.engine.network.Client;
import ritzow.solomon.engine.network.Protocol;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.entity.Entity;
import ritzow.solomon.engine.world.entity.PlayerEntity;

public class PlayerController extends Controller implements KeyHandler {
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
				client.send(Protocol.PlayerAction.buildPlayerMovementAction(Protocol.PlayerAction.PLAYER_UP, true));
			} else if(action == GLFW_RELEASE) {
				player.setUp(false);
				client.send(Protocol.PlayerAction.buildPlayerMovementAction(Protocol.PlayerAction.PLAYER_UP, false));
			}
			break;
		case Controls.KEYBIND_RIGHT:
			if(action == GLFW_PRESS) {
				player.setRight(true);
				client.send(Protocol.PlayerAction.buildPlayerMovementAction(Protocol.PlayerAction.PLAYER_RIGHT, true));
			} else if(action == GLFW_RELEASE) {
				player.setRight(false);
				client.send(Protocol.PlayerAction.buildPlayerMovementAction(Protocol.PlayerAction.PLAYER_RIGHT, false));
			}
			break;
		case Controls.KEYBIND_LEFT:
			if(action == GLFW_PRESS) {
				player.setLeft(true);
				client.send(Protocol.PlayerAction.buildPlayerMovementAction(Protocol.PlayerAction.PLAYER_LEFT, true));
			} else if(action == GLFW_RELEASE) {
				player.setLeft(false);
				client.send(Protocol.PlayerAction.buildPlayerMovementAction(Protocol.PlayerAction.PLAYER_LEFT, false));
			}
			break;
		case Controls.KEYBIND_DOWN:
			if(action == GLFW_PRESS) {
				player.setDown(true);
				client.send(Protocol.PlayerAction.buildPlayerMovementAction(Protocol.PlayerAction.PLAYER_DOWN, true));
			} else if(action == GLFW_RELEASE) {
				player.setDown(false);
				client.send(Protocol.PlayerAction.buildPlayerMovementAction(Protocol.PlayerAction.PLAYER_DOWN, false));
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
