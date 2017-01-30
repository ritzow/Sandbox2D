package ritzow.solomon.engine.input;

import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowFocusCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowIconifyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowRefreshCallback;

import java.util.LinkedList;
import java.util.List;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowIconifyCallback;
import org.lwjgl.glfw.GLFWWindowRefreshCallback;
import ritzow.solomon.engine.input.handler.CursorPosHandler;
import ritzow.solomon.engine.input.handler.FramebufferSizeHandler;
import ritzow.solomon.engine.input.handler.KeyHandler;
import ritzow.solomon.engine.input.handler.MouseButtonHandler;
import ritzow.solomon.engine.input.handler.ScrollHandler;
import ritzow.solomon.engine.input.handler.WindowCloseHandler;
import ritzow.solomon.engine.input.handler.WindowFocusHandler;
import ritzow.solomon.engine.input.handler.WindowIconifyHandler;
import ritzow.solomon.engine.input.handler.WindowRefreshHandler;

/** Dispatches GLFW events to handlers in an object oriented fashion **/
public final class InputManager {
	private List<CursorPosHandler> cursorPosHandlers = 				new LinkedList<CursorPosHandler>();
	private List<FramebufferSizeHandler> framebufferSizeHandlers = 	new LinkedList<FramebufferSizeHandler>();
	private List<KeyHandler> keyHandlers =							new LinkedList<KeyHandler>();
	private List<MouseButtonHandler> mouseButtonHandlers = 			new LinkedList<MouseButtonHandler>();
	private List<ScrollHandler> scrollHandlers = 					new LinkedList<ScrollHandler>();
	private List<WindowRefreshHandler> windowRefreshHandlers = 		new LinkedList<WindowRefreshHandler>();
	private List<WindowCloseHandler> windowCloseHandlers = 			new LinkedList<WindowCloseHandler>();
	private List<WindowIconifyHandler> windowIconifyHandlers = 		new LinkedList<WindowIconifyHandler>();
	private List<WindowFocusHandler> windowFocusHandlers = 			new LinkedList<WindowFocusHandler>();
	
	public InputManager(long window) {
		//TODO use a thread pool executor service with lambda expressions to send events to other threads?
		
		glfwSetKeyCallback(window, new GLFWKeyCallback() {
		    public void invoke (long window, int key, int scancode, int action, int mods) {
				for(KeyHandler handler : keyHandlers) {
					handler.keyboardButton(key, scancode, action, mods);
				}
		    }
		});
		
		glfwSetScrollCallback(window, new GLFWScrollCallback() {
			public void invoke(long window, double xoffset, double yoffset) {
				for(ScrollHandler handler : scrollHandlers) {
					handler.mouseScroll(xoffset, yoffset);
				}
			}
		});
		
		glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
			public void invoke(long window, int button, int action, int mods) {
				for(MouseButtonHandler handler : mouseButtonHandlers) {
					handler.mouseButton(button, action, mods);
				}
			}
		});
		
		glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
			public void invoke(long window, double xpos, double ypos) {
				for(CursorPosHandler handler : cursorPosHandlers) {
					handler.cursorPos(xpos, ypos);
				}
			}
		});
		
		glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
			public void invoke(long window, int width, int height) {
				for(FramebufferSizeHandler handler : framebufferSizeHandlers) {
					handler.framebufferSize(width, height);
				}
			}
		});
		
		glfwSetWindowRefreshCallback(window, new GLFWWindowRefreshCallback() {
			public void invoke(long window) {
				for(WindowRefreshHandler handler : windowRefreshHandlers) {
					handler.windowRefresh();
				}
			}
		});
		
		glfwSetWindowCloseCallback(window, new GLFWWindowCloseCallback() {
			public void invoke(long window) {
				for(WindowCloseHandler handler : windowCloseHandlers) {
					handler.windowClose();
				}
			}
		});
		
		glfwSetWindowIconifyCallback(window, new GLFWWindowIconifyCallback() {
			public void invoke(long window, boolean iconified) {
				for(WindowIconifyHandler handler : windowIconifyHandlers) {
					handler.windowIconify(iconified);
				}
			}
		});
		
		glfwSetWindowFocusCallback(window, new GLFWWindowFocusCallback() {
			public void invoke(long window, boolean focused) {
				for(WindowFocusHandler handler : windowFocusHandlers) {
					handler.windowFocus(focused);
				}
			}
		});
	}
	
	public final void unlinkAll() {
		cursorPosHandlers.clear();
		framebufferSizeHandlers.clear();
		keyHandlers.clear();
		mouseButtonHandlers.clear();
		scrollHandlers.clear();
		windowRefreshHandlers.clear();
		windowCloseHandlers.clear();
		windowIconifyHandlers.clear();
		windowFocusHandlers.clear();
	}

	public final List<CursorPosHandler> getCursorPosHandlers() {
		return cursorPosHandlers;
	}

	public final List<FramebufferSizeHandler> getFramebufferSizeHandlers() {
		return framebufferSizeHandlers;
	}

	public final List<KeyHandler> getKeyHandlers() {
		return keyHandlers;
	}

	public final List<MouseButtonHandler> getMouseButtonHandlers() {
		return mouseButtonHandlers;
	}

	public final List<ScrollHandler> getScrollHandlers() {
		return scrollHandlers;
	}

	public final List<WindowRefreshHandler> getWindowRefreshHandlers() {
		return windowRefreshHandlers;
	}

	public final List<WindowCloseHandler> getWindowCloseHandlers() {
		return windowCloseHandlers;
	}

	public final List<WindowIconifyHandler> getWindowIconifyHandlers() {
		return windowIconifyHandlers;
	}

	public final List<WindowFocusHandler> getWindowFocusHandlers() {
		return windowFocusHandlers;
	}
}