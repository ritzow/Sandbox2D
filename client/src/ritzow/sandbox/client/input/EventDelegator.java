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

import java.util.ArrayList;
import java.util.Collection;
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
public final class EventDelegator {
	public EventDelegator(long window) {
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
	
	private final Collection<CursorPosHandler> cursorPosHandlers = 				new ArrayList<>();
	private final Collection<FramebufferSizeHandler> framebufferSizeHandlers = 	new ArrayList<>();
	private final Collection<KeyHandler> keyHandlers =							new ArrayList<>();
	private final Collection<MouseButtonHandler> mouseButtonHandlers = 			new ArrayList<>();
	private final Collection<ScrollHandler> scrollHandlers = 					new ArrayList<>();
	private final Collection<WindowRefreshHandler> windowRefreshHandlers = 		new ArrayList<>();
	private final Collection<WindowCloseHandler> windowCloseHandlers = 			new ArrayList<>();
	private final Collection<WindowIconifyHandler> windowIconifyHandlers = 		new ArrayList<>();
	private final Collection<WindowFocusHandler> windowFocusHandlers = 			new ArrayList<>();

	public final Collection<CursorPosHandler> cursorPosHandlers() {
		return cursorPosHandlers;
	}

	public final Collection<FramebufferSizeHandler> framebufferSizeHandlers() {
		return framebufferSizeHandlers;
	}

	public final Collection<KeyHandler> keyboardHandlers() {
		return keyHandlers;
	}

	public final Collection<MouseButtonHandler> mouseButtonHandlers() {
		return mouseButtonHandlers;
	}

	public final Collection<ScrollHandler> scrollHandlers() {
		return scrollHandlers;
	}

	public final Collection<WindowRefreshHandler> windowRefreshHandlers() {
		return windowRefreshHandlers;
	}

	public final Collection<WindowCloseHandler> windowCloseHandlers() {
		return windowCloseHandlers;
	}

	public final Collection<WindowIconifyHandler> windowIconifyHandlers() {
		return windowIconifyHandlers;
	}

	public final Collection<WindowFocusHandler> windowFocusHandlers() {
		return windowFocusHandlers;
	}
}