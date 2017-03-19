package ritzow.solomon.engine.world.base;

import ritzow.solomon.engine.util.Exitable;

public final class BlockGridUpdater implements Runnable, Exitable {
	private volatile boolean exit, finished;
	private final BlockGrid blocks;
	//private final World world;
	
	public BlockGridUpdater(World world, BlockGrid blocks) {
		//this.world = world;
		this.blocks = blocks;
	}

	@Override
	public void run() {
		int height = blocks.getHeight();
		int width = blocks.getWidth();
		while(!exit) {
			for(int row = 0; row < height; row++) {	
				for(int column = 0; column < width; column++) {
					
				}
				
				try {
					Thread.sleep(1); //sleep after each row
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}
	
	@Override
	public void exit() {
		exit = true;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

}
