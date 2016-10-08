package graphics;

import static org.lwjgl.glfw.GLFW.*;

import input.*;
import org.lwjgl.glfw.GLFWErrorCallback;

public final class Display {
	
	protected InputManager input;
	
	protected long displayID;
	protected int screenWidth;
	protected int screenHeight;
	
	protected int windowedX;
	protected int windowedY;
	protected int windowedWidth;
	protected int windowedHeight;
	
	protected volatile boolean shouldClose;
	
	protected int[] framebufferWidth = new int[1];
	protected int[] framebufferHeight = new int[1];
	protected double[] cursorX = new double[1];
	protected double[] cursorY = new double[1];
	
	public Display(String title) {
		glfwInit();
		GLFWErrorCallback.createPrint(System.err).set();

		screenWidth = glfwGetVideoMode(glfwGetPrimaryMonitor()).width();
		screenHeight = glfwGetVideoMode(glfwGetPrimaryMonitor()).height();
		
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);	
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		
		displayID = glfwCreateWindow(screenWidth/2, screenHeight/2, title, 0, 0);
		glfwSetWindowSizeLimits(displayID, screenWidth/2, screenHeight/2, screenWidth, screenHeight);
		glfwSetWindowPos(displayID, screenWidth - screenWidth/2 - screenWidth/4, screenHeight - screenHeight/2 - screenHeight/4);		
		
		input = new InputManager(displayID);
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
	
	public boolean getShouldClose() {
		return shouldClose;
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
			int[] storeH = new int[1];
			int[] storeV = new int[1];
			glfwGetWindowSize(displayID, storeH, storeV);
			windowedWidth = storeH[0];
			windowedHeight = storeV[0];
			glfwGetWindowPos(displayID, storeH, storeV);
			windowedX = storeH[0];
			windowedY = storeV[0];
			
			glfwSetWindowMonitor(displayID, glfwGetPrimaryMonitor(), 0, 0, screenWidth, screenHeight, GLFW_DONT_CARE);
			glfwFocusWindow(displayID);
		}
		
		else {
			glfwSetWindowMonitor(displayID, 0, windowedX, windowedY, windowedWidth, windowedHeight, GLFW_DONT_CARE);
		}
	}
}
