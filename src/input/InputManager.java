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
	
	public InputManager(long window) {
		glfwSetKeyCallback(window, glfwKeyCallback);
		glfwSetScrollCallback(window, glfwScrollCallback);
		glfwSetMouseButtonCallback(window, glfwMouseButtonCallback);
		glfwSetCursorPosCallback(window, glfwCursorPosCallback);
		glfwSetFramebufferSizeCallback(window, glfwFramebufferSizeCallback);
		glfwSetWindowRefreshCallback(window, glfwWindowRefreshCallback);
		glfwSetWindowCloseCallback(window, glfwWindowCloseCallback);
	}
	
	public void add(InputHandler handler) {
		if(handler instanceof FramebufferSizeHandler) framebufferSizeHandlers.add((FramebufferSizeHandler)handler);
		if(handler instanceof KeyHandler) keyHandlers.add((KeyHandler)handler);
		if(handler instanceof ScrollHandler) scrollHandlers.add((ScrollHandler)handler);
		if(handler instanceof WindowCloseHandler) windowCloseHandlers.add((WindowCloseHandler)handler);
		if(handler instanceof WindowRefreshHandler) windowRefreshHandlers.add((WindowRefreshHandler)handler);
		if(handler instanceof MouseButtonHandler) mouseButtonHandlers.add((MouseButtonHandler)handler);
		if(handler instanceof CursorPosHandler) cursorPosHandlers.add((CursorPosHandler)handler);
	}
	
	public void remove(InputHandler handler) {
		if(handler instanceof FramebufferSizeHandler) framebufferSizeHandlers.remove((FramebufferSizeHandler)handler);
		if(handler instanceof KeyHandler) keyHandlers.remove((KeyHandler)handler);
		if(handler instanceof ScrollHandler) scrollHandlers.remove((ScrollHandler)handler);
		if(handler instanceof WindowCloseHandler) windowCloseHandlers.remove((WindowCloseHandler)handler);
		if(handler instanceof WindowRefreshHandler) windowRefreshHandlers.remove((WindowRefreshHandler)handler);
		if(handler instanceof MouseButtonHandler) mouseButtonHandlers.remove((MouseButtonHandler)handler);
		if(handler instanceof CursorPosHandler) cursorPosHandlers.remove((CursorPosHandler)handler);
	}
	
	public final ArrayList<WindowCloseHandler> getWindowCloseHandlers() {
		return windowCloseHandlers;
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
	
	private GLFWWindowCloseCallback glfwWindowCloseCallback = new GLFWWindowCloseCallback() {
		public void invoke(long window) {
			for(WindowCloseHandler handler : windowCloseHandlers) {
				handler.windowClose();
			}
		}
	};
	
	private GLFWCursorPosCallback glfwCursorPosCallback = new GLFWCursorPosCallback() {
		public void invoke(long window, double xpos, double ypos) {
			for(CursorPosHandler handler : cursorPosHandlers) {
				handler.cursorPos(xpos, ypos);
			}
		}
	};
	
	private GLFWFramebufferSizeCallback glfwFramebufferSizeCallback = new GLFWFramebufferSizeCallback() {
		public void invoke(long window, int width, int height) {
			for(FramebufferSizeHandler handler : framebufferSizeHandlers) {
				handler.framebufferSize(width, height);
			}
		}
	};
	
	private GLFWKeyCallback glfwKeyCallback = new GLFWKeyCallback() {
	    public void invoke (long window, int key, int scancode, int action, int mods) {
			for(KeyHandler handler : keyHandlers) {
				handler.keyboardButton(key, scancode, action, mods);
			}
	    }
	};
	
	private GLFWMouseButtonCallback glfwMouseButtonCallback = new GLFWMouseButtonCallback() {
		public void invoke(long window, int button, int action, int mods) {
			for(MouseButtonHandler handler : mouseButtonHandlers) {
				handler.mouseButton(button, action, mods);
			}
		}
	};
	
	private GLFWScrollCallback glfwScrollCallback = new GLFWScrollCallback() {
		public void invoke(long window, double xoffset, double yoffset) {
			for(ScrollHandler handler : scrollHandlers) {
				handler.mouseScroll(xoffset, yoffset);
			}
		}
	};
	
	private GLFWWindowRefreshCallback glfwWindowRefreshCallback = new GLFWWindowRefreshCallback() {
		public void invoke(long window) {
			for(WindowRefreshHandler handler : windowRefreshHandlers) {
				handler.windowRefresh();
			}
		}
	};
}