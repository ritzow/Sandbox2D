package graphics;

import static org.lwjgl.glfw.GLFW.*;

import input.InputManager;

public final class Display {
	
	protected long displayID;
	protected InputManager input;
	
	protected int windowedX;
	protected int windowedY;
	protected int windowedWidth;
	protected int windowedHeight;
	
	public Display(String title) {
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);	
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		
		int screenWidth = glfwGetVideoMode(glfwGetPrimaryMonitor()).width();
		int screenHeight = glfwGetVideoMode(glfwGetPrimaryMonitor()).height();
		
		displayID = glfwCreateWindow(screenWidth/2, screenHeight/2, title, 0, 0);
		glfwSetWindowPos(displayID, screenWidth/4, screenHeight/4);
		
		input = new InputManager(displayID);
	}
	
	public void setCursor(long cursor) {
		glfwSetCursor(displayID, cursor);
	}
	
	public void setContext() {
		glfwMakeContextCurrent(displayID);
	}
	
	public void closeContext() {
		glfwMakeContextCurrent(0);
	}
	
	public void refresh() {
		glfwSwapBuffers(displayID);
	}
	
	public void pollEvents() {
		glfwPollEvents();
	}
	
	public void waitEvents() {
		glfwWaitEvents();
	}
	
	public void destroy() {
		glfwDestroyWindow(displayID);
	}
	
	public boolean getFullscreen() {
		return glfwGetWindowMonitor(displayID) != 0;
	}
	
	public InputManager getInputManager() {
		return input;
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

	public void setFullscreen(boolean fullscreen) {
		if(fullscreen) {
			int[] storeH = new int[1], storeV = new int[1];
			glfwGetWindowSize(displayID, storeH, storeV);
			windowedWidth = storeH[0]; windowedHeight = storeV[0];
			glfwGetWindowPos(displayID, storeH, storeV);
			windowedX = storeH[0]; windowedY = storeV[0];
			
			glfwSetWindowMonitor(displayID, glfwGetPrimaryMonitor(), 0, 0,
					glfwGetVideoMode(glfwGetPrimaryMonitor()).width(), 
					glfwGetVideoMode(glfwGetPrimaryMonitor()).height(), GLFW_DONT_CARE);
			glfwFocusWindow(displayID);
		}
		
		else {
			glfwSetWindowMonitor(displayID, 0, windowedX, windowedY, windowedWidth, windowedHeight, GLFW_DONT_CARE);
		}
	}
}
