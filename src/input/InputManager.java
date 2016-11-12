package input;

import org.lwjgl.glfw.*;

import static org.lwjgl.glfw.GLFW.*;

import input.handler.*;

import java.util.ArrayList;

public final class InputManager {
	
	private ArrayList<CursorPosHandler> cursorPosHandlers = new ArrayList<CursorPosHandler>();
	private ArrayList<FramebufferSizeHandler> framebufferSizeHandlers = new ArrayList<FramebufferSizeHandler>();
	private ArrayList<KeyHandler> keyHandlers = new ArrayList<KeyHandler>();
	private ArrayList<MouseButtonHandler> mouseButtonHandlers = new ArrayList<MouseButtonHandler>();
	private ArrayList<ScrollHandler> scrollHandlers = new ArrayList<ScrollHandler>();
	private ArrayList<WindowRefreshHandler> windowRefreshHandlers = new ArrayList<WindowRefreshHandler>();
	private ArrayList<WindowCloseHandler> windowCloseHandlers = new ArrayList<WindowCloseHandler>();
	private ArrayList<WindowIconifyHandler> windowIconifyHandlers = new ArrayList<WindowIconifyHandler>();
	private ArrayList<WindowFocusHandler> windowFocusHandlers = new ArrayList<WindowFocusHandler>();
	
	public InputManager(long window) {
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

	public final ArrayList<CursorPosHandler> getCursorPosHandlers() {
		return cursorPosHandlers;
	}

	public final ArrayList<FramebufferSizeHandler> getFramebufferSizeHandlers() {
		return framebufferSizeHandlers;
	}

	public final ArrayList<KeyHandler> getKeyHandlers() {
		return keyHandlers;
	}

	public final ArrayList<MouseButtonHandler> getMouseButtonHandlers() {
		return mouseButtonHandlers;
	}

	public final ArrayList<ScrollHandler> getScrollHandlers() {
		return scrollHandlers;
	}

	public final ArrayList<WindowRefreshHandler> getWindowRefreshHandlers() {
		return windowRefreshHandlers;
	}

	public final ArrayList<WindowCloseHandler> getWindowCloseHandlers() {
		return windowCloseHandlers;
	}

	public final ArrayList<WindowIconifyHandler> getWindowIconifyHandlers() {
		return windowIconifyHandlers;
	}

	public final ArrayList<WindowFocusHandler> getWindowFocusHandlers() {
		return windowFocusHandlers;
	}
}