package input.controller;

import input.InputManager;
import input.handler.KeyHandler;
import input.handler.MouseButtonHandler;
import input.handler.ScrollHandler;
import util.Updatable;

public abstract class CameraController extends Controller implements KeyHandler, ScrollHandler, MouseButtonHandler, Updatable {

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
