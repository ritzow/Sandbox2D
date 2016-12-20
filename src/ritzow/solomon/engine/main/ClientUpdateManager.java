package ritzow.solomon.engine.main;

import java.util.ArrayList;
import java.util.List;
import ritzow.solomon.engine.input.InputManager;
import ritzow.solomon.engine.input.handler.WindowFocusHandler;
import ritzow.solomon.engine.util.Exitable;
import ritzow.solomon.engine.util.Updatable;

/**
 * Updates all updatable objects stored in the updatables list
 * @author Solomon Ritzow
 *
 */
public final class ClientUpdateManager implements Runnable, Exitable, WindowFocusHandler {
	private volatile boolean exit, finished, focused;
	
	private final List<Updatable> updatables;
	
	public ClientUpdateManager() {
		updatables = new ArrayList<Updatable>();
	}
	
	public List<Updatable> getUpdatables() {
		return updatables;
	}

	@Override
	public void run() {
		try {
			while(!exit) {
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
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
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
