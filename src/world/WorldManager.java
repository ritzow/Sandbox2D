package world;

import util.Exitable;
import util.Synchronizer;

public final class WorldManager implements Runnable, Exitable {
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
		
		new Thread(blockManagerForeground, "Foreground Block Updater " + world.hashCode()).start();
		new Thread(blockManagerBackground, "Background Block Updater " + world.hashCode()).start();
		
		try {
			long currentTime;
			float updateTime;
			long previousTime = System.nanoTime();
			
			while(!exit) {
			    currentTime = System.nanoTime();
			    updateTime = (currentTime - previousTime) * 0.0000000625f; //convert from nanoseconds to sixteenth of a milliseconds
			    previousTime = currentTime;
				
				world.update(updateTime);
				Thread.sleep((long)Math.max(1, 16 - updateTime));
			}
		} catch (InterruptedException e) {
			System.err.println("World update loop was interrupted");
		} finally {
			Synchronizer.waitForExit(blockManagerForeground);
			Synchronizer.waitForExit(blockManagerBackground);
			
			synchronized(this) {
				finished = true;
				notifyAll();
			}
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