package ritzow.sandbox.client.graphics;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.input.*;

import static org.lwjgl.glfw.GLFW.*;

public final class Display implements InputProvider {
	private final long displayID;

	private int windowedX;
	private int windowedY;
	private int windowedWidth;
	private int windowedHeight;

	public Display(int GLVersionMajor, int GLVersionMinor, String title, GLFWImage... icons) {
		glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GLVersionMajor);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GLVersionMinor);
		glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_FALSE);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
		glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_TRUE);

		GLFWVidMode video = glfwGetVideoMode(glfwGetPrimaryMonitor());
		if(video == null) {
			throw new RuntimeException("failed to get video mode");
		} else {
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

	private static void updateVsync() {
		glfwSwapInterval(StandardClientOptions.VSYNC ? 1 : 0);
	}

	public void hideCursor() {
		glfwSetInputMode(displayID, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
	}

	public void showCursor() {
		glfwSetInputMode(displayID, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
	}

	public void disableCursor() {
		glfwSetInputMode(displayID, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
	}

	public void resetCursor() {
		setCursor(0);
	}

	public void setCursor(long cursor) {
		glfwSetCursor(displayID, cursor);
	}

	/** Call from OpenGL thread **/
	public GLCapabilities setGraphicsContextOnThread() {
		glfwMakeContextCurrent(displayID);
		GL.create();
		return GL.createCapabilities();
	}

	public boolean wasClosed() {
		return glfwWindowShouldClose(displayID);
	}

	@Override
	public boolean isControlActivated(Button buttonControl) {
		return switch(buttonControl.type()) {
			case Control.BUTTON_TYPE_MOUSE -> glfwGetMouseButton(displayID, buttonControl.code()) == GLFW_PRESS;
			case Control.BUTTON_TYPE_KEYBOARD -> glfwGetKey(displayID, buttonControl.code()) == GLFW_PRESS;
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
		updateVsync();
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
			updateVsync();
		} else {
			glfwSetWindowMonitor(displayID, 0, windowedX, windowedY, windowedWidth, windowedHeight, GLFW_DONT_CARE);
			updateVsync();
		}
		focus();
	}

	//TODO add a barrier around this and run all of the main thread code on a different thread when this is blocking?
	public void handleEvents(ControlsContext store) {
		store.nextFrame();
		this.context = store;
		glfwPollEvents();
	}

	private ControlsContext context = new ControlsContext();

	private void setupCallbacks() {
		glfwSetKeyCallback(displayID, (windowID, key, scancode, action, mods) -> {
			switch(action) {
				case GLFW_PRESS -> context.press(Control.BUTTON_TYPE_KEYBOARD, key);
				case GLFW_RELEASE -> context.release(Control.BUTTON_TYPE_KEYBOARD, key);
				case GLFW_REPEAT -> context.repeat(Control.BUTTON_TYPE_KEYBOARD, key);
			}
		});

		glfwSetMouseButtonCallback(displayID, (windowID, button, action, mods) -> {
			switch(action) {
				case GLFW_PRESS -> context.press(Control.BUTTON_TYPE_MOUSE, button);
				case GLFW_RELEASE -> context.release(Control.BUTTON_TYPE_MOUSE, button);
			}
		});

		//TODO implement mouse scroll polling, interaction with ControlsContext
		glfwSetCharCallback(displayID, (windowID, codePoint) -> context.onCharacterTyped(codePoint));
		glfwSetScrollCallback(displayID, (windowID, xoffset, yoffset) -> context.mouseScroll(xoffset, yoffset));
		glfwSetFramebufferSizeCallback(displayID, (windowID, width, height) -> context.framebufferSize(width, height));
		glfwSetWindowRefreshCallback(displayID, windowID -> context.windowRefresh());
		glfwSetWindowCloseCallback(displayID, windowID -> context.windowClose());
		glfwSetWindowIconifyCallback(displayID, (windowID, iconified) -> context.windowIconify(iconified));
		glfwSetWindowFocusCallback(displayID, (windowID, focused) -> context.windowFocus(focused));
	}
}
