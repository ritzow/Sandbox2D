package ritzow.sandbox.client.input;

import static org.lwjgl.glfw.GLFW.*;

public class Control {
	public static final Button QUIT = 			new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_ESCAPE);
	public static final Button FULLSCREEN =		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_F11);
	public static final Button CONNECT =		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_C);	
	public static final Button ACTIVATE_UI_ELEMENT = 	new Button(Button.TYPE_MOUSE, GLFW_MOUSE_BUTTON_LEFT);

	public static final Button INCREASEZOOM = 	new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_EQUAL);
	public static final Button DECREASEZOOM = 	new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_MINUS);
	public static final Button RESET_ZOOM = 	new Button(Button.TYPE_MOUSE, GLFW_MOUSE_BUTTON_MIDDLE);
	
	public static final Button SCROLL_MODIFIER = new Button(Button.TYPE_KEYBOARD, GLFW_KEY_RIGHT_SHIFT);
	public static final Button USE_HELD_ITEM = 	new Button(Button.TYPE_MOUSE, GLFW_MOUSE_BUTTON_LEFT);
	public static final Button THROW_BOMB = 	new Button(Button.TYPE_KEYBOARD, GLFW_KEY_ENTER);
	
	public static final Button MOVE_UP = 			new Button(Button.TYPE_KEYBOARD, GLFW_KEY_UP);
	public static final Button MOVE_DOWN = 			new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_DOWN);
	public static final Button MOVE_LEFT = 			new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_LEFT);
	public static final Button MOVE_RIGHT = 	 		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_RIGHT);
	
	public static final Button SLOT_SELECT_1 =		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_1);
	public static final Button SLOT_SELECT_2 =		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_2);
	public static final Button SLOT_SELECT_3 =		new Button(Button.TYPE_KEYBOARD,  GLFW_KEY_3);
	

	public static class Button {
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
			return this == obj || (obj instanceof Button &&
					buttonType == ((Button)obj).buttonType &&
					buttonCode == ((Button)obj).buttonCode);
		}
	}
}
