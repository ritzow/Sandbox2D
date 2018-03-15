package ritzow.sandbox.client.input;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwPostEmptyEvent;

import java.io.IOException;
import org.lwjgl.glfw.GLFWErrorCallback;
import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.ui.Cursors;
import ritzow.sandbox.util.Utility;

/** Initializes GLFW, loads mouse cursors, processes all GLFW events until exited **/
public final class EventProcessor implements Runnable {
	
	/** status markers for the state of the event manager **/
	private volatile boolean setupComplete, shouldDisplay, exit;
	
	/** the window to use this event manager with **/
	private Display display;
	private final Object lock = new Object();
	
	@Override
	public void run() {
		if(!glfwInit())
			throw new UnsupportedOperationException("GLFW failed to initialize");
		GLFWErrorCallback.createPrint(System.err).set();
		
		try {
			Cursors.loadAll();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		display = new Display("2D Game");
		display.setCursor(Cursors.PICKAXE);

		setupComplete = true;
		Utility.notify(lock);
		
		Utility.waitOnCondition(lock, () -> shouldDisplay);
		display.setFullscreen(true);
		
		while(!exit) {
			display.waitEvents();
		}
		
		display.destroy();
		glfwTerminate();
	}
	
	public void waitForSetup() {
		Utility.waitOnCondition(lock, () -> setupComplete);
	}
	
	public void setReadyToDisplay() {
		shouldDisplay = true;
		Utility.notify(lock);
	}
	
	public void stop() {
		exit = true;
		glfwPostEmptyEvent();
	}
	
	public Display getDisplay() {
		if(display == null)
			throw new IllegalStateException("event processor is not running yet and hasn't been initialized");
		return display;
	}
}