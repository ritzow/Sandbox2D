package ritzow.sandbox.world;

import ritzow.sandbox.util.Service;

public final class WorldUpdater implements Service {
	private final World world;
	private volatile boolean setup, exit, finished;
	
	public WorldUpdater(World world) {
		this.world = world;
	}
	
	@Override
	public void run() {
		BlockGridUpdater blockManagerForeground = new BlockGridUpdater(world, world.getForeground());
		BlockGridUpdater blockManagerBackground = new BlockGridUpdater(world, world.getBackground());
		new Thread(blockManagerForeground, "Foreground Block Updater").start();
		new Thread(blockManagerBackground, "Background Block Updater").start();
		
		synchronized(this) {
			setup = true;
			this.notifyAll();
		}
		
		try {
			long currentTime;
			long previousTime = System.nanoTime();
			while(!exit) {
			    currentTime = System.nanoTime();
			    world.update((currentTime - previousTime) * 0.0000000625f); //convert from nanoseconds to sixteenths of milliseconds
			    previousTime = currentTime;
				Thread.sleep(16); //TODO change based on time it takes to update (computer speed, slower on older computers, faster on newer)
			}
		} catch (InterruptedException e) {
			System.err.println("World update loop was interrupted");
		} finally {
			blockManagerForeground.waitForExit();
			blockManagerBackground.waitForExit();
			synchronized(this) {
				finished = true;
				notifyAll();
			}
		}
	}
	
	public World getWorld() {
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