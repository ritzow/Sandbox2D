package world;

import util.Exitable;
import util.Synchronizer;
import util.TimeUtil;

public final class WorldManager implements Runnable, Exitable {
	private static final long MILLISECONDS_PER_UPDATE = 16;
	private volatile World world;
	private volatile boolean exit;
	private volatile boolean finished;
	
	public WorldManager(World world) {
		this.world = world;
	}
	
	@Override
	public void run() {
		BlockGridManager blockManager = new BlockGridManager(world.getBlocks());
		
		new Thread(blockManager).start();
		
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
		
		Synchronizer.waitForExit(blockManager);
		
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