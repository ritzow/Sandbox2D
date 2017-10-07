package ritzow.sandbox.server.world;

import ritzow.sandbox.util.Service;
import ritzow.sandbox.world.World;

public final class WorldUpdater<W extends World> implements Service {
	private final W world;
	private volatile boolean setup, exit, finished;
	
	private static final float MAX_TIMESTEP = 2;
	
	public WorldUpdater(W world) {
		this.world = world;
	}
	
	@Override
	public void run() {
		synchronized(this) {
			setup = true;
			this.notifyAll();
		}
		
		try {
			long previousTime = System.nanoTime();
			while(!exit) {
			    long currentTime = System.nanoTime();
			    float updateTime = (currentTime - previousTime) * 0.0000000625f;
				for(float time; updateTime > 0; updateTime -= time) {
					time = Math.min(updateTime, MAX_TIMESTEP);
					world.update(time);
					updateTime -= time;
				}
			    previousTime = currentTime;
				Thread.sleep(16); //TODO alter sleep time based on time it takes for each update?
			}
		} catch (InterruptedException e) {
			System.err.println("World update loop was interrupted");
		} finally {
			synchronized(this) {
				finished = true;
				notifyAll();
			}
		}
	}
	
	public W getWorld() {
		return world;
	}
	
	@Override
	public void exit() {
		exit = true;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public boolean isSetupComplete() {
		return setup;
	}
}