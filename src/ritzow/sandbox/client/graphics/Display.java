package ritzow.sandbox.client.graphics;

import static org.lwjgl.glfw.GLFW.*;

import ritzow.sandbox.client.input.InputManager;

public final class Display {
	private final long displayID;
	private final InputManager input;
	private final RenderManager render;
	
	private int windowedX;
	private int windowedY;
	private int windowedWidth;
	private int windowedHeight;
	
	public Display(String title) {
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);	
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		
		int screenWidth = glfwGetVideoMode(glfwGetPrimaryMonitor()).width();
		int screenHeight = glfwGetVideoMode(glfwGetPrimaryMonitor()).height();
		
		displayID = glfwCreateWindow(screenWidth/2, screenHeight/2, title, 0, 0);
		glfwSetWindowPos(displayID, screenWidth/4, screenHeight/4);
		
		input = new InputManager(displayID);
		render = new RenderManager(this);
	}
	
	public void setCursor(long cursor) {
		glfwSetCursor(displayID, cursor);
	}
	
	public void setGraphicsContextOnThread() {
		glfwMakeContextCurrent(displayID);
	}
	
	@SuppressWarnings("static-method")
	public void closeContext() {
		glfwMakeContextCurrent(0);
	}
	
	public void refresh() {
		glfwSwapBuffers(displayID);
	}
	
	@SuppressWarnings("static-method")
	public void pollEvents() {
		glfwPollEvents();
	}
	
	@SuppressWarnings("static-method")
	public void waitEvents() {
		glfwWaitEvents();
	}
	
	public void destroy() {
		input.unlinkAll();
		glfwDestroyWindow(displayID);
	}
	
	public boolean getFullscreen() {
		return glfwGetWindowMonitor(displayID) != 0;
	}
	
	public InputManager getInputManager() {
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

	public void setFullscreen(boolean fullscreen) {
		if(fullscreen) {
			if(glfwGetWindowMonitor(displayID) == 0) {
				//create buffers to store window properties
				int[] storeH = new int[1], storeV = new int[1];
				
				//store window size in storeH and storeV
				glfwGetWindowSize(displayID, storeH, storeV);
				
				//store storeH and storeV into fields
				windowedWidth = storeH[0]; 	windowedHeight = storeV[0];
				
				//repeat with window position
				glfwGetWindowPos(displayID, storeH, storeV);
				windowedX = storeH[0]; 		windowedY = storeV[0];
			}
			
			//make the window fullscreen
			int width = glfwGetVideoMode(glfwGetPrimaryMonitor()).width();
			int height = glfwGetVideoMode(glfwGetPrimaryMonitor()).height();
			int refreshRate = glfwGetVideoMode(glfwGetPrimaryMonitor()).refreshRate();
			glfwSetWindowMonitor(displayID, glfwGetPrimaryMonitor(), 0, 0, width, height, refreshRate);
			glfwFocusWindow(displayID);
		} else {
			glfwSetWindowMonitor(displayID, 0, windowedX, windowedY, windowedWidth, windowedHeight, GLFW_DONT_CARE);
		}
	}
}
