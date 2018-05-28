package ritzow.sandbox.client.input;

import java.util.HashSet;
import java.util.Set;
import ritzow.sandbox.client.graphics.Display;

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
	
	public static Sandbox2DGameplayContext createGLFWGameplayContext(Display window) {
		return new GLFWSandbox2DGameplayContext(window);
	}
	
	public static class Sandbox2DGameplayContext {
		public final AxisControl ZOOM_CONTROL;
		
		public Sandbox2DGameplayContext(AxisControl zoom) {
			ZOOM_CONTROL = zoom;
		}
	}
	
	private static final class GLFWSandbox2DGameplayContext extends Sandbox2DGameplayContext {
		static final class zoom extends ControlBase<AxisControlHandler> implements AxisControl {
			public zoom(Display display) {
				display.getInputManager().scrollHandlers().add((x, y) -> {
					for(AxisControlHandler handler : handlers) {
						handler.axisChangeEvent(y);
					}
				});
			}
		}
		
		private GLFWSandbox2DGameplayContext(Display display) {
			super(new zoom(display));
		}
	}
	
	public interface AxisControlHandler {
		void axisChangeEvent(double delta);
	}
	
	public interface AxisControl extends Control<AxisControlHandler> {
		
	}
	
	public interface Control<T> {
		Set<T> handlers();
	}
	
	private static abstract class ControlBase<H> implements Control<H> {
		protected final Set<H> handlers;
		private ControlBase() {
			handlers = new HashSet<>();
		}
		
		public Set<H> handlers() {
			return handlers;
		}
	}
}
