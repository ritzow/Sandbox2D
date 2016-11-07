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
		BlockGridManager blockManagerForeground = new BlockGridManager(world.getForeground());
		BlockGridManager blockManagerBackground = new BlockGridManager(world.getBackground());
		
		new Thread(blockManagerForeground).start();
		new Thread(blockManagerBackground).start();
		
		try {	
			long start;
			while(!exit) {
				start = System.nanoTime();
				world.update(1f);
				Thread.sleep((long)Math.max(0, TimeUtil.getElapsedTime(start, System.nanoTime()) + MILLISECONDS_PER_UPDATE));
			}
		} catch(InterruptedException e) {
			
		}
		
		Synchronizer.waitForExit(blockManagerForeground);
		Synchronizer.waitForExit(blockManagerBackground);
		
		
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