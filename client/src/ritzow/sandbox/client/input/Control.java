package ritzow.sandbox.client.input;

import static org.lwjgl.glfw.GLFW.*;

public interface Control {
	Button QUIT = 			new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_ESCAPE);
	Button FULLSCREEN =		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_F11);
	Button CONNECT =		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_C);
	Button ACTIVATE_UI_ELEMENT = 	new Button(Button.TYPE_MOUSE, GLFW_MOUSE_BUTTON_LEFT);

	Button INCREASEZOOM = 	new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_EQUAL);
	Button DECREASEZOOM = 	new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_MINUS);
	Button RESET_ZOOM = 	new Button(Button.TYPE_MOUSE, GLFW_MOUSE_BUTTON_MIDDLE);

	Button SCROLL_MODIFIER = new Button(Button.TYPE_KEYBOARD, GLFW_KEY_RIGHT_SHIFT);
	Button USE_HELD_ITEM = 	new Button(Button.TYPE_MOUSE, GLFW_MOUSE_BUTTON_LEFT);
	Button THROW_BOMB = 	new Button(Button.TYPE_KEYBOARD, GLFW_KEY_ENTER);

	Button MOVE_UP = 			new Button(Button.TYPE_KEYBOARD, GLFW_KEY_UP);
	Button MOVE_DOWN = 			new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_DOWN);
	Button MOVE_LEFT = 			new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_LEFT);
	Button MOVE_RIGHT = 	 		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_RIGHT);

	Button SLOT_SELECT_1 =		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_1);
	Button SLOT_SELECT_2 =		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_2);
	Button SLOT_SELECT_3 =		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_3);

	byte type();
	int code();

	class Button implements Control {
		public static final byte TYPE_MOUSE = 0, TYPE_KEYBOARD = 1;

		public byte buttonType;
		public int buttonCode;

		public Button(byte type, int code) {
			this.buttonType = type;
			this.buttonCode = code;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + buttonCode;
			result = prime * result + buttonType;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
					obj instanceof Button b &&
					buttonType == b.buttonType &&
					buttonCode == b.buttonCode);
		}

		@Override
		public byte type() {
			return buttonType;
		}

		@Override
		public int code() {
			return buttonCode;
		}
	}
}
