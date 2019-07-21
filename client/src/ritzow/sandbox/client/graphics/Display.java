package ritzow.sandbox.client.graphics;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.Control.Button;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.input.InputProvider;

public final class Display implements InputProvider {
	private final long displayID;

	private int windowedX;
	private int windowedY;
	private int windowedWidth;
	private int windowedHeight;

	public Display(int GLVersionMajor, int GLVersionMinor, boolean core, String title, GLFWImage... icons) {
		glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GLVersionMajor);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GLVersionMinor);
		glfwWindowHint(GLFW_OPENGL_PROFILE, core ? GLFW_OPENGL_CORE_PROFILE : GLFW_OPENGL_ANY_PROFILE);
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
			for(GLFWImage icon : icons) {
				buffer.put(icon);
			}
			glfwSetWindowIcon(displayID, buffer.flip());
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

	@Override
	public boolean isControlActivated(Button buttonControl) {
		return switch(buttonControl.buttonType) {
			case Control.Button.TYPE_MOUSE -> glfwGetMouseButton(displayID, buttonControl.buttonCode) == GLFW_PRESS;
			case Control.Button.TYPE_KEYBOARD -> glfwGetKey(displayID, buttonControl.buttonCode) == GLFW_PRESS;
			default -> false;
		};
	}

	/** Call from OpenGL thread thread **/
	public void refresh() {
		glfwSwapBuffers(displayID);
	}

	/** Call from main thread **/
	public void destroy() {
		glfwDestroyWindow(displayID);
	}

	@Override
	public int getCursorX() {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			DoubleBuffer width = stack.mallocDouble(1);
			glfwGetCursorPos(displayID, width, null);
			return (int)width.get();
		}
	}

	@Override
	public int getCursorY() {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			DoubleBuffer height = stack.mallocDouble(1);
			glfwGetCursorPos(displayID, null, height);
			return (int)height.get();
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
		return glfwGetWindowAttrib(displayID, GLFW_ICONIFIED) == GLFW_TRUE;
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

	public void poll(InputContext input) {
		handler = input;
		glfwPollEvents();
	}

	private InputContext handler = new InputContext() {};

	private void setupCallbacks() {
		glfwSetKeyCallback(displayID, (windowID, key, scancode, action, mods) -> {
			handler.keyboardButton(key, scancode, action, mods);
			if(action == GLFW_PRESS) {
				handler.buttonControls().getOrDefault(new Button(Control.Button.TYPE_KEYBOARD, key), () -> {}).run();
			}
		});

		glfwSetScrollCallback(displayID, (windowID, xoffset, yoffset) -> {
			handler.mouseScroll(xoffset, yoffset);
		});

		glfwSetMouseButtonCallback(displayID, (windowID, button, action, mods) -> {
			handler.mouseButton(button, action, mods);
			if(action == GLFW_PRESS) {
				handler.buttonControls().getOrDefault(new Button(Control.Button.TYPE_MOUSE, button), () -> {}).run();
			}
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
