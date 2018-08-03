package ritzow.sandbox.client.graphics;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.IntBuffer;
import org.lwjgl.system.MemoryStack;
import ritzow.sandbox.client.input.EventDelegator;

public final class Display {
	private final long displayID;
	private final EventDelegator input;
	private final RenderManager render;

	private int windowedX;
	private int windowedY;
	private int windowedWidth;
	private int windowedHeight;

	public Display(String title, int GLVersionMajor, int GLVersionMinor) {
		glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GLVersionMajor);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GLVersionMinor);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

		int screenWidth = glfwGetVideoMode(glfwGetPrimaryMonitor()).width();
		int screenHeight = glfwGetVideoMode(glfwGetPrimaryMonitor()).height();

		displayID = glfwCreateWindow(screenWidth/2, screenHeight/2, title, 0, 0);
		glfwSetWindowPos(displayID, screenWidth/4, screenHeight/4);

		input = new EventDelegator(displayID);
		render = new RenderManager(this);

		//input.windowFocusHandlers().add(focused -> this.focused = focused);
	}

	public void setCursor(long cursor) {
		glfwSetCursor(displayID, cursor);
	}

	/** Call from OpenGL thread **/
	public void setGraphicsContextOnThread() {
		glfwMakeContextCurrent(displayID);
	}

	/** Call from OpenGL thread thread **/
	public void refresh() {
		glfwSwapBuffers(displayID);
	}

	/** Call from main thread **/
	public void destroy() {
		glfwDestroyWindow(displayID);
	}

	public boolean getFullscreen() {
		return glfwGetWindowMonitor(displayID) != 0;
	}

	public EventDelegator getEventDelegator() {
		return input;
	}

	public RenderManager getRenderManager() {
		return render;
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

	public boolean focused() {
		return glfwGetWindowAttrib(displayID, GLFW_FOCUSED) == GLFW_TRUE;
	}

	public int width() {
		try(MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
			IntBuffer buffer = stack.mallocInt(1);
			glfwGetFramebufferSize(displayID, buffer, null);
			return buffer.get(0);
		}
	}

	public int height() {
		try(MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
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
				try(MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
					IntBuffer first = stack.mallocInt(1), second = stack.mallocInt(1);
					glfwGetWindowSize(displayID, first, second);
					windowedWidth = first.get(0);
					windowedHeight = second.get(0);
					glfwGetWindowPos(displayID, first, second);
					windowedX = first.get(0);
					windowedY = second.get(0);
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
}
