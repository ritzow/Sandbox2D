package ritzow.sandbox.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.handler.WindowFocusHandler;

/**
 * Updates all updatable objects stored in the updatables list
 * @author Solomon Ritzow
 *
 */
public final class ClientUpdater implements Runnable, Exitable, WindowFocusHandler {
	private volatile boolean focused, exit, finished;
	
	private final List<Updatable> updatables;
	
	public ClientUpdater() {
		updatables = new LinkedList<Updatable>();
	}
	
	public Collection<Updatable> getUpdatables() {
		return updatables;
	}
	
	public void start() {
		new Thread(this, "Client Updater").start();
	}
	
	public synchronized void stop() {
		exit = true;
		notifyAll();
	}

	@Override
	public void run() {
		try {
			while(!exit) {
				if(focused) {
					for(Updatable u : updatables) {
						u.update();
					}
					Thread.sleep(1);
				} else {
					synchronized(this) {
						//pauses updating when window is not active to reduce idle CPU usage
						while(!(focused || exit)) {
							wait();
						}
					}
				}
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
		} finally {
			synchronized(this) {
				finished = true;
				notifyAll();
			}
		}
	}
	
	@Override
	public synchronized void exit() {
		exit = true;
		notifyAll();
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public synchronized void windowFocus(boolean focused) {
		//if the window just became focused, notify the updater to stop waiting
		if(this.focused = focused) {
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
