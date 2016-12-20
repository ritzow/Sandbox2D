package ritzow.solomon.engine.input.handler;

public interface KeyHandler extends InputHandler {
	public void keyboardButton(int key, int scancode, int action, int mods);
}