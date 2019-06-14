package ritzow.sandbox.client.graphics;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.IntBuffer;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import ritzow.sandbox.client.input.InputContext;

public final class Display {
	private final long displayID;

	private int windowedX;
	private int windowedY;
	private int windowedWidth;
	private int windowedHeight;

	public Display(int GLVersionMajor, int GLVersionMinor, String title, GLFWImage... icons) {
		glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GLVersionMajor);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GLVersionMinor);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_TRUE);
		
		GLFWVidMode video = glfwGetVideoMode(glfwGetPrimaryMonitor());
		int screenWidth = video.width();
		int screenHeight = video.height();

		displayID = glfwCreateWindow(screenWidth/2, screenHeight/2, title, 0, 0);
		if(displayID == 0) {
			throw new UnsupportedOperationException("Error creating window");
		}
		glfwSetWindowPos(displayID, screenWidth/4, screenHeight/4);
		setIcons(icons);
		setupCallbacks();
	}
	
	private void setIcons(GLFWImage... icons) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			var buffer = GLFWImage.mallocStack(icons.length, stack);
			for(int i = 0; i < icons.length; i++) {
				buffer.put(icons[i]);
			}
			buffer.flip();
			glfwSetWindowIcon(displayID, buffer);
		}
	}
	
	public void enableCursor(boolean enabled) {
		glfwSetInputMode(displayID, GLFW_CURSOR, enabled ? GLFW_CURSOR_NORMAL : GLFW_CURSOR_DISABLED);
	}
	
	public void resetCursor() {
		setCursor(0);
	}

	public void setCursor(long cursor) {
		glfwSetCursor(displayID, cursor);
	}

	/** Call from OpenGL thread **/
	public void setGraphicsContextOnThread() {
		glfwMakeContextCurrent(displayID);
	}
	
	public boolean wasClosed() {
		return glfwWindowShouldClose(displayID);
	}

	public void poll(InputContext input) {
		handler = input;
		glfwPollEvents();
	}
	
	/** Call from OpenGL thread thread **/
	public void refresh() {
		glfwSwapBuffers(displayID);
	}

	/** Call from main thread **/
	public void destroy() {
		glfwDestroyWindow(displayID);
	}
	
	public static final class Dimensions {
		public final int x, y;
		
		Dimensions(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
	public Dimensions getSize() {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer width = stack.mallocInt(1), height = stack.mallocInt(1);
			glfwGetFramebufferSize(displayID, width, height);
			return new Dimensions(width.get(), height.get());
		}
	}

	public boolean getFullscreen() {
		return glfwGetWindowMonitor(displayID) != 0;
	}

	public void setVisible(boolean visible) {
		if(visible) {
			glfwShowWindow(displayID);
		} else {
			glfwHideWindow(displayID);
		}
	}

	public void setResolution(int width, int height) {
		glfwSetWindowSize(displayID, width, height);
	}

	public void show() {
		glfwShowWindow(displayID);
	}

	public void focus() {
		glfwFocusWindow(displayID);
	}

	public boolean minimized() {
		return glfwGetWindowAttrib(displayID, GLFW_ICONIFIED) == 1;
	}
	
	public boolean focused() {
		return glfwGetWindowAttrib(displayID, GLFW_FOCUSED) == GLFW_TRUE;
	}

	public int width() {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer buffer = stack.mallocInt(1);
			glfwGetFramebufferSize(displayID, buffer, null);
			return buffer.get(0);
		}
	}

	public int height() {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer buffer = stack.mallocInt(1);
			glfwGetFramebufferSize(displayID, null, buffer);
			return buffer.get(0);
		}
	}

	public boolean isKeyPressed(int keyboardKey) {
		return glfwGetKey(displayID, keyboardKey) == GLFW_PRESS;
	}

	public void toggleFullscreen() {
		setFullscreen(!getFullscreen());
	}

	public void setFullscreen(boolean fullscreen) {
		if(fullscreen) {
			if(glfwGetWindowMonitor(displayID) == 0) { //on fullscreen, store windowed information
				try(MemoryStack stack = MemoryStack.stackPush()) {
					IntBuffer width = stack.mallocInt(1), height = stack.mallocInt(1);
					glfwGetWindowSize(displayID, width, height);
					windowedWidth = width.get(0);
					windowedHeight = height.get(0);
					glfwGetWindowPos(displayID, width, height);
					windowedX = width.get(0);
					windowedY = height.get(0);
				}
			}

			//make the window fullscreen
			long monitor = glfwGetPrimaryMonitor();
			int width = glfwGetVideoMode(monitor).width();
			int height = glfwGetVideoMode(monitor).height();
			int refreshRate = glfwGetVideoMode(monitor).refreshRate();
			glfwSetWindowMonitor(displayID, monitor, 0, 0, width, height, refreshRate);
			focus();
		} else {
			glfwSetWindowMonitor(displayID, 0, windowedX, windowedY, windowedWidth, windowedHeight, GLFW_DONT_CARE);
			focus();
		}
	}
	
	private InputContext handler = new InputContext() {};
	
	private void setupCallbacks() {
		glfwSetKeyCallback(displayID, (windowID, key, scancode, action, mods) -> {
			handler.keyboardButton(key, scancode, action, mods);
		});
		
		glfwSetScrollCallback(displayID, (windowID, xoffset, yoffset) -> {
			handler.mouseScroll(xoffset, yoffset);
		});
		
		glfwSetMouseButtonCallback(displayID, (windowID, button, action, mods) -> {
			handler.mouseButton(button, action, mods);
		});
		
		glfwSetCursorPosCallback(displayID, (windowID, xpos, ypos) -> {
			handler.cursorPos(xpos, ypos);
		});
		
		glfwSetFramebufferSizeCallback(displayID, (windowID, width, height) -> {
			handler.framebufferSize(width, height);
		});
		
		glfwSetWindowRefreshCallback(displayID, windowID -> {
			handler.windowRefresh();
		});
		
		glfwSetWindowCloseCallback(displayID, windowID -> {
			handler.windowClose();
		});
		
		glfwSetWindowIconifyCallback(displayID, (windowID, iconified) -> {
			handler.windowIconify(iconified);
		});
		
		glfwSetWindowFocusCallback(displayID, (windowID, focused) -> {
			handler.windowFocus(focused);
		});
	}
}
