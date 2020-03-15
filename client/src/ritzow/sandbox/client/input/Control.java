package ritzow.sandbox.client.input;

import static org.lwjgl.glfw.GLFW.*;

public interface Control {
	byte BUTTON_TYPE_MOUSE = 0, BUTTON_TYPE_KEYBOARD = 1;

	byte type();
	int code();

	record Button(byte type, int code) implements Control {}

	Button QUIT = 			new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_ESCAPE);
	Button FULLSCREEN =		new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_F11);
	Button CONNECT =		new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_C);
	//Button ACTIVATE_UI_ELEMENT = 	new Button(BUTTON_TYPE_MOUSE, GLFW_MOUSE_BUTTON_LEFT);

	Button INCREASEZOOM = 	new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_EQUAL);
	Button DECREASEZOOM = 	new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_MINUS);
	Button RESET_ZOOM = 	new Button(BUTTON_TYPE_MOUSE, GLFW_MOUSE_BUTTON_MIDDLE);

	Button SCROLL_MODIFIER = new Button(BUTTON_TYPE_KEYBOARD, GLFW_KEY_RIGHT_SHIFT);
	Button USE_HELD_ITEM = 	new Button(BUTTON_TYPE_MOUSE, GLFW_MOUSE_BUTTON_LEFT);
	Button THROW_BOMB = 	new Button(BUTTON_TYPE_KEYBOARD, GLFW_KEY_ENTER);

	Button MOVE_UP = 			new Button(BUTTON_TYPE_KEYBOARD, GLFW_KEY_UP);
	Button MOVE_DOWN = 			new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_DOWN);
	Button MOVE_LEFT = 			new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_LEFT);
	Button MOVE_RIGHT = 	 		new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_RIGHT);

	Button SLOT_SELECT_1 =		new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_1);
	Button SLOT_SELECT_2 =		new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_2);
	Button SLOT_SELECT_3 =		new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_3);
}
