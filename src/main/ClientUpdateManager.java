package main;

import java.util.ArrayList;
import util.Exitable;
import util.Updatable;

public final class ClientUpdateManager implements Runnable, Exitable {
	private volatile boolean finished;
	private boolean exit;
	
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
				for(int i = 0; i < updatables.size(); i++) {
					updatables.get(i).update();
				}
				Thread.sleep(1);
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
	
}
