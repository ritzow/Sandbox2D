package ritzow.solomon.engine.input.controller;

import ritzow.solomon.engine.input.InputManager;
import ritzow.solomon.engine.input.handler.KeyHandler;
import ritzow.solomon.engine.input.handler.MouseButtonHandler;
import ritzow.solomon.engine.input.handler.ScrollHandler;

public abstract class CameraController implements Controller, KeyHandler, ScrollHandler, MouseButtonHandler {

	@Override
	public void link(InputManager manager) {
		manager.getKeyHandlers().add(this);
		manager.getScrollHandlers().add(this);
		manager.getMouseButtonHandlers().add(this);
	}

	@Override
	public void unlink(InputManager manager) {
		manager.getKeyHandlers().remove(this);
		manager.getScrollHandlers().remove(this);
		manager.getMouseButtonHandlers().remove(this);
	}

}
