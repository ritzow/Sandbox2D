package ritzow.sandbox.client.input;

import java.util.Map;

/** Handles window events **/
@SuppressWarnings(value="unused")
public interface InputContext {
	InputContext EMPTY_CONTEXT = new InputContext() {};

	default Map<Button, Runnable> buttonControls() {return Map.of();}
	default void framebufferSize(int width, int height) {}
	default void keyboardButton(int key, int scancode, int action, int mods) {}
	default void mouseButton(int button, int action, int mods) {}
	default void mouseScroll(double xoffset, double yoffset) {}
	default void windowClose() {}
	default void windowFocus(boolean focused) {}
	default void windowIconify(boolean iconified) {}
	default void windowRefresh() {}
}
