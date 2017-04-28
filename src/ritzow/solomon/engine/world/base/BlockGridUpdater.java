package ritzow.solomon.engine.world.base;

import ritzow.solomon.engine.util.Exitable;
import ritzow.solomon.engine.world.block.Block;

public final class BlockGridUpdater implements Runnable, Exitable {
	private volatile boolean exit, finished;
	private final BlockGrid blocks;
	private final World world;
	
	public BlockGridUpdater(World world, BlockGrid blocks) {
		this.world = world;
		this.blocks = blocks;
	}

	@Override
	public void run() {
		int height = blocks.getHeight();
		int width = blocks.getWidth();
		long currentTime;
		long previousTime = System.nanoTime();
		while(!exit) {
			try {
			    currentTime = System.nanoTime();
			    float updateTime = (currentTime - previousTime) * 0.0000000625f;
				for(int row = 0; row < height; row++) {	
					for(int column = 0; column < width; column++) {
						Block block = blocks.get(column, row);
						if(block != null) {
							block.update(world, updateTime);
						}
					}
					previousTime = currentTime;
				}
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		synchronized(this) {
			finished = true;
			notifyAll();
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
