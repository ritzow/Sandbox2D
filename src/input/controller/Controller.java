package input.controller;

import input.InputManager;

public abstract class Controller {
	public abstract void link(InputManager input);
	public abstract void unlink(InputManager input);
}
