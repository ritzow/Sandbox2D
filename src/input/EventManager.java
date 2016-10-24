package input;

import static org.lwjgl.glfw.GLFW.glfwTerminate;

import graphics.Display;
import util.Exitable;
import util.Installable;

/**
 * EventThread objects must only be run on the main thread
 * @author Solomon
 *
 */
public final class EventManager implements Runnable, Installable, Exitable {
	private volatile boolean setupComplete;
	private volatile boolean finished;
	private volatile boolean exit;
	private volatile boolean shouldDisplay;
	
	private Display display;
	
	@Override
	public void run() {
		display = new Display("2D Game");
		
		synchronized(this) {
			setupComplete = true;
			notifyAll();
		}
		
		waitUntilReady();
		display.setFullscreen(true);
		
		synchronized(this) {
			while(!exit) {
				display.waitEvents();
			}
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
	
	public void setReadyToDisplay() {
		shouldDisplay = true;
	}
	
	private void waitUntilReady() {
		synchronized(this) {
			try {
				while(!shouldDisplay) {
					this.wait();
				}
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public boolean isSetupComplete() {
		return setupComplete;
	}
	
	@Override
	public void exit() {
		exit = true;
		
		synchronized(this) {
			notifyAll();
		}
	}

	@Override
	public boolean isFinished() {
		return finished;
	}
}