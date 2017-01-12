package ritzow.solomon.engine.input;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwPostEmptyEvent;

import java.io.IOException;
import org.lwjgl.glfw.GLFWErrorCallback;
import ritzow.solomon.engine.graphics.Display;
import ritzow.solomon.engine.resource.Cursors;
import ritzow.solomon.engine.util.Exitable;
import ritzow.solomon.engine.util.Installable;

/**
 * Initializes GLFW, loads mouse cursors, processes all GLFW events
 * @author Solomon Ritzow
 *
 */
public final class EventManager implements Runnable, Installable, Exitable {
	
	/** status markers for the state of the event manager thread **/
	private volatile boolean setupComplete, shouldDisplay, exit, finished;
	
	/** the window to use this event manager with **/
	private Display display;
	
	@Override
	public void run() {
		
		if(!glfwInit()) {
			throw new UnsupportedOperationException("GLFW failed to initialize");
		}
		
		GLFWErrorCallback.createPrint(System.err).set();
		
		try {
			Cursors.loadAll();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		display = new Display("2D Game");
		display.setCursor(Cursors.PICKAXE);
		
		synchronized(this) {
			setupComplete = true;
			notifyAll();
		}
		
		waitUntilReady();
		display.setFullscreen(true);
		
		while(!exit) {
			display.waitEvents();
		}
		
		display.destroy();
		glfwTerminate();

		synchronized(this) {
			finished = true;
			notifyAll();
		}
	}
	
	public Display getDisplay() {
		return display;
	}
	
	public synchronized void setReadyToDisplay() {
		shouldDisplay = true;
		this.notifyAll();
	}
	
	private synchronized void waitUntilReady() {
		try {
			while(!shouldDisplay) {
				this.wait();
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean isSetupComplete() {
		return setupComplete;
	}
	
	@Override
	public synchronized void exit() {
		exit = true;
		glfwPostEmptyEvent();
		this.notifyAll();
	}

	@Override
	public boolean isFinished() {
		return finished;
	}
}