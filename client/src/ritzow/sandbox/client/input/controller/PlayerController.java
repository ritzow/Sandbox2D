package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;

import org.lwjgl.glfw.GLFW;
import ritzow.sandbox.client.Client;
import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.network.Protocol.PlayerAction;

public class PlayerController implements Controller, KeyHandler {
	protected final Client client;
	
	public PlayerController(Client client) {
		this.client = client;
	}

	public void link(InputManager manager) {
		manager.getKeyHandlers().add(this);
	}

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
		ClientPlayerEntity player = client.getPlayer();
		PlayerAction playerAction;
		
		switch(key) {
		case GLFW.GLFW_KEY_W:
		case ControlScheme.KEYBIND_UP:
			playerAction = PlayerAction.MOVE_UP; break;
		case GLFW.GLFW_KEY_D:
		case ControlScheme.KEYBIND_RIGHT:
			playerAction = PlayerAction.MOVE_RIGHT; break;
		case GLFW.GLFW_KEY_A:
		case ControlScheme.KEYBIND_LEFT:
			playerAction = PlayerAction.MOVE_LEFT; break;
		case GLFW.GLFW_KEY_S:
		case ControlScheme.KEYBIND_DOWN:
			playerAction = PlayerAction.MOVE_DOWN; break;
		default:
			return;
		}
		
		client.sendPlayerAction(playerAction, enabled);
		player.processAction(playerAction, enabled);
	}
}
