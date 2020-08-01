package ritzow.sandbox.client.input;

import static org.lwjgl.glfw.GLFW.*;
import static ritzow.sandbox.client.data.StandardClientOptions.LEFTY;

public interface Control {
	byte BUTTON_TYPE_MOUSE = 0, BUTTON_TYPE_KEYBOARD = 1;

	byte type();
	int code();

	Button UI_ACTIVATE = 		new Button(BUTTON_TYPE_MOUSE, GLFW_MOUSE_BUTTON_LEFT);
	Button UI_CONTEXT = 		new Button(BUTTON_TYPE_MOUSE, GLFW_MOUSE_BUTTON_RIGHT);
	Button UI_TERTIARY = 		new Button(BUTTON_TYPE_MOUSE, GLFW_MOUSE_BUTTON_MIDDLE);
	Button UI_BACKSPACE =       new Button(Control.BUTTON_TYPE_KEYBOARD, GLFW_KEY_BACKSPACE);
	Button UI_DELETE =          new Button(Control.BUTTON_TYPE_KEYBOARD, GLFW_KEY_DELETE);

	Button QUIT = 				new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_ESCAPE);
	Button FULLSCREEN =			new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_F11);

	Button ZOOM_INCREASE = 		new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_EQUAL);
	Button ZOOM_DECREASE = 		new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_MINUS);
	Button ZOOM_RESET = 		new Button(BUTTON_TYPE_MOUSE, GLFW_MOUSE_BUTTON_MIDDLE);

	Button SCROLL_MODIFIER = 	new Button(BUTTON_TYPE_KEYBOARD, GLFW_KEY_RIGHT_SHIFT);
	Button USE_HELD_ITEM = 		new Button(BUTTON_TYPE_MOUSE, GLFW_MOUSE_BUTTON_LEFT);
	Button THROW_BOMB = 		new Button(BUTTON_TYPE_KEYBOARD, GLFW_KEY_ENTER);

	Button MOVE_UP = 			new Button(BUTTON_TYPE_KEYBOARD, LEFTY ? GLFW_KEY_UP : GLFW_KEY_W);
	Button MOVE_DOWN = 			new Button(BUTTON_TYPE_KEYBOARD, LEFTY ? GLFW_KEY_DOWN : GLFW_KEY_S);
	Button MOVE_LEFT = 			new Button(BUTTON_TYPE_KEYBOARD, LEFTY ? GLFW_KEY_LEFT : GLFW_KEY_A);
	Button MOVE_RIGHT = 	 	new Button(BUTTON_TYPE_KEYBOARD, LEFTY ? GLFW_KEY_RIGHT : GLFW_KEY_D);

	Button SLOT_SELECT_1 =		new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_KP_1);
	Button SLOT_SELECT_2 =		new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_KP_2);
	Button SLOT_SELECT_3 =		new Button(BUTTON_TYPE_KEYBOARD,  GLFW_KEY_KP_3);
}
