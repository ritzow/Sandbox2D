package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static ritzow.sandbox.network.Protocol.PlayerAction.*;

import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.client.input.EventDelegator;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.network.GameTalker;

public class PlayerController implements KeyHandler {
	protected final GameTalker client;

	public PlayerController(GameTalker client) {
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
			var send = switch(key) {
				case ControlScheme.KEYBIND_UP 	-> action == GLFW_PRESS ? MOVE_UP_START : MOVE_UP_STOP;
				case ControlScheme.KEYBIND_RIGHT -> action == GLFW_PRESS ? MOVE_RIGHT_START : MOVE_RIGHT_STOP;
				case ControlScheme.KEYBIND_LEFT -> action == GLFW_PRESS ? MOVE_LEFT_START : MOVE_LEFT_STOP;
				case ControlScheme.KEYBIND_DOWN -> action == GLFW_PRESS ? MOVE_DOWN_START : MOVE_DOWN_STOP;
				default -> null;
			};
			
			if(send != null)
				client.sendPlayerAction(send);
		}
	}
}
