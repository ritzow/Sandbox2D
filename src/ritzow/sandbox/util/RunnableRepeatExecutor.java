package ritzow.sandbox.util;

import java.util.Collection;
import java.util.LinkedList;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.handler.WindowFocusHandler;

/**
 * Updates all updatable objects stored in the updatables list
 * @author Solomon Ritzow
 *
 */
public final class RunnableRepeatExecutor implements Runnable, Exitable, WindowFocusHandler {
	private volatile boolean focused, exit, finished;
	private volatile long delay;
	
	private final Collection<Runnable> runnables;
	
	public RunnableRepeatExecutor() {
		runnables = new LinkedList<Runnable>();
	}
	
	public Collection<Runnable> getRunnables() {
		return runnables;
	}
	
	public void start() {
		new Thread(this, "Client Updater").start();
	}
	
	public synchronized void stop() {
		exit = true;
		notifyAll();
	}

	public void setRepeatDelay(long millseconds) {
		this.delay = Math.max(0, delay);
	}
	
	@Override
	public void run() {
		try {
			while(!exit) {
				if(focused) {
					for(Runnable r : runnables) {
						r.run();
					}
					Thread.sleep(delay);
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
