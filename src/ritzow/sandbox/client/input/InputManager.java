package ritzow.sandbox.client.input;

import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowFocusCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowIconifyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowRefreshCallback;

import java.util.Collection;
import java.util.LinkedList;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowIconifyCallback;
import org.lwjgl.glfw.GLFWWindowRefreshCallback;
import ritzow.sandbox.client.input.handler.CursorPosHandler;
import ritzow.sandbox.client.input.handler.FramebufferSizeHandler;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.input.handler.MouseButtonHandler;
import ritzow.sandbox.client.input.handler.ScrollHandler;
import ritzow.sandbox.client.input.handler.WindowCloseHandler;
import ritzow.sandbox.client.input.handler.WindowFocusHandler;
import ritzow.sandbox.client.input.handler.WindowIconifyHandler;
import ritzow.sandbox.client.input.handler.WindowRefreshHandler;

/** Dispatches GLFW events to handlers in an object oriented fashion **/
public final class InputManager {
	private final Collection<CursorPosHandler> cursorPosHandlers = 				new LinkedList<CursorPosHandler>();
	private final Collection<FramebufferSizeHandler> framebufferSizeHandlers = 	new LinkedList<FramebufferSizeHandler>();
	private final Collection<KeyHandler> keyHandlers =							new LinkedList<KeyHandler>();
	private final Collection<MouseButtonHandler> mouseButtonHandlers = 			new LinkedList<MouseButtonHandler>();
	private final Collection<ScrollHandler> scrollHandlers = 					new LinkedList<ScrollHandler>();
	private final Collection<WindowRefreshHandler> windowRefreshHandlers = 		new LinkedList<WindowRefreshHandler>();
	private final Collection<WindowCloseHandler> windowCloseHandlers = 			new LinkedList<WindowCloseHandler>();
	private final Collection<WindowIconifyHandler> windowIconifyHandlers = 		new LinkedList<WindowIconifyHandler>();
	private final Collection<WindowFocusHandler> windowFocusHandlers = 			new LinkedList<WindowFocusHandler>();
	
	public InputManager(long window) {
		//TODO use a thread pool executor service with lambda expressions to send events to other threads?
		
		glfwSetKeyCallback(window, (windowID, key, scancode, action, mods) -> {
			for(KeyHandler handler : keyHandlers) {
				handler.keyboardButton(key, scancode, action, mods);
			}
		});
		
		glfwSetScrollCallback(window, new GLFWScrollCallback() {
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				for(ScrollHandler handler : scrollHandlers) {
					handler.mouseScroll(xoffset, yoffset);
				}
			}
		});
		
		glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {
				for(MouseButtonHandler handler : mouseButtonHandlers) {
					handler.mouseButton(button, action, mods);
				}
			}
		});
		
		glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double xpos, double ypos) {
				for(CursorPosHandler handler : cursorPosHandlers) {
					handler.cursorPos(xpos, ypos);
				}
			}
		});
		
		glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				for(FramebufferSizeHandler handler : framebufferSizeHandlers) {
					handler.framebufferSize(width, height);
				}
			}
		});
		
		glfwSetWindowRefreshCallback(window, new GLFWWindowRefreshCallback() {
			@Override
			public void invoke(long window) {
				for(WindowRefreshHandler handler : windowRefreshHandlers) {
					handler.windowRefresh();
				}
			}
		});
		
		glfwSetWindowCloseCallback(window, new GLFWWindowCloseCallback() {
			@Override
			public void invoke(long window) {
				for(WindowCloseHandler handler : windowCloseHandlers) {
					handler.windowClose();
				}
			}
		});
		
		glfwSetWindowIconifyCallback(window, new GLFWWindowIconifyCallback() {
			@Override
			public void invoke(long window, boolean iconified) {
				for(WindowIconifyHandler handler : windowIconifyHandlers) {
					handler.windowIconify(iconified);
				}
			}
		});
		
		glfwSetWindowFocusCallback(window, new GLFWWindowFocusCallback() {
			@Override
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

	public final Collection<CursorPosHandler> getCursorPosHandlers() {
		return cursorPosHandlers;
	}

	public final Collection<FramebufferSizeHandler> getFramebufferSizeHandlers() {
		return framebufferSizeHandlers;
	}

	public final Collection<KeyHandler> getKeyHandlers() {
		return keyHandlers;
	}

	public final Collection<MouseButtonHandler> getMouseButtonHandlers() {
		return mouseButtonHandlers;
	}

	public final Collection<ScrollHandler> getScrollHandlers() {
		return scrollHandlers;
	}

	public final Collection<WindowRefreshHandler> getWindowRefreshHandlers() {
		return windowRefreshHandlers;
	}

	public final Collection<WindowCloseHandler> getWindowCloseHandlers() {
		return windowCloseHandlers;
	}

	public final Collection<WindowIconifyHandler> getWindowIconifyHandlers() {
		return windowIconifyHandlers;
	}

	public final Collection<WindowFocusHandler> getWindowFocusHandlers() {
		return windowFocusHandlers;
	}
}