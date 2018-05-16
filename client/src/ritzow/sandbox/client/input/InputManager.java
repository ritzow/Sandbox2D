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
import ritzow.sandbox.client.input.handler.*;

/** Dispatches GLFW events to handlers in an object oriented fashion **/
public final class InputManager {
	private final Collection<CursorPosHandler> cursorPosHandlers = 				new LinkedList<>();
	private final Collection<FramebufferSizeHandler> framebufferSizeHandlers = 	new LinkedList<>();
	private final Collection<KeyHandler> keyHandlers =							new LinkedList<>();
	private final Collection<MouseButtonHandler> mouseButtonHandlers = 			new LinkedList<>();
	private final Collection<ScrollHandler> scrollHandlers = 					new LinkedList<>();
	private final Collection<WindowRefreshHandler> windowRefreshHandlers = 		new LinkedList<>();
	private final Collection<WindowCloseHandler> windowCloseHandlers = 			new LinkedList<>();
	private final Collection<WindowIconifyHandler> windowIconifyHandlers = 		new LinkedList<>();
	private final Collection<WindowFocusHandler> windowFocusHandlers = 			new LinkedList<>();
	
	//TODO replace InputManager stuff with game related abstractions (like primaryAction instead of mouse left click)
	public InputManager(long window) {
		//TODO use a thread pool executor service with lambda expressions to send events to other threads?
		
		glfwSetKeyCallback(window, (windowID, key, scancode, action, mods) -> {
			for(KeyHandler handler : keyHandlers) {
				handler.keyboardButton(key, scancode, action, mods);
			}
		});
		
		glfwSetScrollCallback(window, (windowID, xoffset, yoffset) -> {
			for(ScrollHandler handler : scrollHandlers) {
				handler.mouseScroll(xoffset, yoffset);
			}
		});
		
		glfwSetMouseButtonCallback(window, (windowID, button, action, mods) -> {
			for(MouseButtonHandler handler : mouseButtonHandlers) {
				handler.mouseButton(button, action, mods);
			}
		});
		
		glfwSetCursorPosCallback(window, (windowID, xpos, ypos) -> {
			for(CursorPosHandler handler : cursorPosHandlers) {
				handler.cursorPos(xpos, ypos);
			}
		});
		
		glfwSetFramebufferSizeCallback(window, (windowID, width, height) -> {
			for(FramebufferSizeHandler handler : framebufferSizeHandlers) {
				handler.framebufferSize(width, height);
			}
		});
		
		glfwSetWindowRefreshCallback(window, windowID -> {
			for(WindowRefreshHandler handler : windowRefreshHandlers) {
				handler.windowRefresh();
			}
		});
		
		glfwSetWindowCloseCallback(window, windowID -> {
			for(WindowCloseHandler handler : windowCloseHandlers) {
				handler.windowClose();
			}
		});
		
		glfwSetWindowIconifyCallback(window, (windowID, iconified) -> {
			for(WindowIconifyHandler handler : windowIconifyHandlers) {
				handler.windowIconify(iconified);
			}
		});
		
		glfwSetWindowFocusCallback(window, (windowID, focused) -> {
			for(WindowFocusHandler handler : windowFocusHandlers) {
				handler.windowFocus(focused);
			}
		});
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