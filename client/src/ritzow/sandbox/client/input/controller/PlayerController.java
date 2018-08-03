package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.*;

import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.client.input.EventDelegator;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.network.Protocol.PlayerAction;

public class PlayerController implements KeyHandler {
	protected final Client client;

	public PlayerController(Client client) {
		this.client = client;
	}

	public void link(EventDelegator manager) {
		manager.keyboardHandlers().add(this);
	}

	public void unlink(EventDelegator manager) {
		manager.keyboardHandlers().remove(this);
	}

	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) {
		if(action != GLFW_REPEAT) {
			switch(key) {
			case ControlScheme.KEYBIND_UP:
				client.sendPlayerAction(action == GLFW_PRESS ? PlayerAction.MOVE_UP_START : PlayerAction.MOVE_UP_STOP);
				break;
			case ControlScheme.KEYBIND_RIGHT:
				client.sendPlayerAction(action == GLFW_PRESS ? PlayerAction.MOVE_RIGHT_START : PlayerAction.MOVE_RIGHT_STOP);
				break;
			case ControlScheme.KEYBIND_LEFT:
				client.sendPlayerAction(action == GLFW_PRESS ? PlayerAction.MOVE_LEFT_START : PlayerAction.MOVE_LEFT_STOP);
				break;
			case ControlScheme.KEYBIND_DOWN:
				client.sendPlayerAction(action == GLFW_PRESS ? PlayerAction.MOVE_DOWN_START : PlayerAction.MOVE_DOWN_STOP);
				break;
			}
		}
	}
}
