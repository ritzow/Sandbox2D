package ritzow.solomon.engine.world;

import ritzow.solomon.engine.util.Exitable;

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
		try {
			while(!exit) {
				for(int row = 0; row < blocks.getHeight(); row++) {
					for(int column = 0; column < blocks.getWidth(); column++) {
						if(blocks.isBlock(column, row) && !blocks.isSurrounded(column, row) && !isStable(column, row)) {
							blocks.destroy(world, column, row);
						}
					}
					if(!exit)
						Thread.sleep(1);
				}
			}
		} catch (InterruptedException e) {
			System.out.println("Block Grid Updater interrupted");
		}
		
		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}
	
	private boolean isStable(int x, int y) {
		return (blocks.isBlock(x, y - 1) || blocks.isBlock(x - 1, y) || blocks.isBlock(x + 1, y));
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
