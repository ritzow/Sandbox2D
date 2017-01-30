package ritzow.solomon.engine.util;

import java.util.LinkedList;
import java.util.List;
import ritzow.solomon.engine.input.InputManager;
import ritzow.solomon.engine.input.handler.WindowFocusHandler;

/**
 * Updates all updatable objects stored in the updatables list
 * @author Solomon Ritzow
 *
 */
public final class ClientUpdater implements Runnable, Exitable, WindowFocusHandler {
	private volatile boolean exit, finished, focused;
	
	private final List<Updatable> updatables;
	
	public ClientUpdater() {
		updatables = new LinkedList<Updatable>();
	}
	
	public List<Updatable> getUpdatables() {
		return updatables;
	}

	@Override
	public void run() {
		
		while(!exit) {
			try {
				if(focused) {
					for(int i = 0; i < updatables.size(); i++) {
						updatables.get(i).update();
					}
					Thread.sleep(1);
				} else {
					synchronized(this) {
						while(!focused && !exit) {
							this.wait();
						}
					}
				}
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		synchronized(this) {
			finished = true;
			notifyAll();
		}
	}
	
	public void exit() {
		exit = true;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public synchronized void windowFocus(boolean focused) {
		this.focused = focused;
		if(focused) {
			notifyAll();
		}
	}

	@Override
	public void link(InputManager manager) {
		manager.getWindowFocusHandlers().add(this);
	}

	@Override
	public void unlink(InputManager manager) {
		manager.getWindowFocusHandlers().remove(this);
	}
	
}
