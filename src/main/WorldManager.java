package main;

import util.TimeUtil;
import world.World;

public final class WorldManager implements Runnable, Exitable {
	private final long MILLISECONDS_PER_UPDATE = 16;
	private volatile World world;
	private volatile boolean exit;
	private volatile boolean finished;
	
	public WorldManager(World world) {
		this.world = world;
	}
	
	@Override
	public void run() {
		try {
			long start;
			while(!exit) {
				start = System.nanoTime();
				world.update(1);
				Thread.sleep((long)Math.max(0, TimeUtil.getElapsedTime(start, System.nanoTime()) + MILLISECONDS_PER_UPDATE));
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		synchronized(this) {
			finished = true;
			notifyAll();
		}
	}
	
	public World getWorld() {
		return world;
	}
	
	public void exit() {
		exit = true;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}
}