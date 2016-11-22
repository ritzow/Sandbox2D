package input;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import graphics.Display;
import java.io.IOException;
import org.lwjgl.glfw.GLFWErrorCallback;
import resource.Cursors;
import util.Exitable;
import util.Installable;

public final class EventManager implements Runnable, Installable, Exitable {
	private volatile boolean setupComplete;
	private volatile boolean finished;
	private volatile boolean exit;
	private volatile boolean shouldDisplay;
	
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
		this.notifyAll();
	}

	@Override
	public boolean isFinished() {
		return finished;
	}
}