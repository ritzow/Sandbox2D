package ritzow.sandbox.client.input;

public class ControlScheme {
	public static final int KEYBIND_UP = 			org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
	public static final int KEYBIND_DOWN = 			org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
	public static final int KEYBIND_LEFT = 			org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
	public static final int KEYBIND_RIGHT = 		org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
	public static final int KEYBIND_INCREASEZOOM = 	org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD;
	public static final int KEYBIND_DECREASEZOOM = 	org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT;
	public static final int KEYBIND_QUIT = 			org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
	public static final int KEYBIND_FULLSCREEN =	org.lwjgl.glfw.GLFW.GLFW_KEY_F11;
	public static final int KEYBIND_ACTIVATE =		org.lwjgl.glfw.GLFW.GLFW_KEY_E;
	
	public static final ButtonControl KEYBIND_DIG = new GLFWMouseButtonControl();
	
	public ControlScheme() {
		
	}
	
	public static interface Control {
		
	}
	
	public static interface ButtonControl extends Control {
		public boolean pressed();
		public void setOnPress(Runnable action);
		public void setOnRelease(Runnable action);
	}
	
	private static class GLFWMouseButtonControl implements ButtonControl {
		
		
		@Override
		public boolean pressed() {
			//TODO implement method 'pressed'
			return false;
		}

		@Override
		public void setOnPress(Runnable action) {
			//TODO implement method 'setOnPress'
			
		}

		@Override
		public void setOnRelease(Runnable action) {
			//TODO implement method 'setOnRelease'
			
		}
	}
}
