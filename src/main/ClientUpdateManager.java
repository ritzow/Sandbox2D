package main;

import input.InputManager;
import input.handler.WindowFocusHandler;
import java.util.ArrayList;
import util.Exitable;
import util.Updatable;

public final class ClientUpdateManager implements Runnable, Exitable, WindowFocusHandler {
	private volatile boolean finished;
	private boolean exit;
	private volatile boolean focused;
	
	private ArrayList<Updatable> updatables;
	
	public ClientUpdateManager() {
		this.updatables = new ArrayList<Updatable>();
	}
	
	public ArrayList<Updatable> getUpdatables() {
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
				}
				
				else {
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
			this.notifyAll();
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
