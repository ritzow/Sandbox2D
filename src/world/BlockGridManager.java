package world;

import util.Exitable;

public final class BlockGridManager implements Runnable, Exitable {
	private volatile boolean exit;
	private volatile boolean finished;
	private BlockGrid blocks;
	
	public BlockGridManager(BlockGrid blocks) {
		this.blocks = blocks;
	}

	@Override
	public void run() {
		while(!exit) {
			//Update all of the blocks
			for(int row = 0; row < blocks.getHeight(); row++) {
				for(int column = 0; column < blocks.getWidth(); column++) {
					if(blocks.isBlock(column, row) && !blocks.isHidden(column, row)) {
						if(row != 0 && !blocks.isStable(column, row)) { //destroy loose blocks
							blocks.destroy(column, row);
						}
					}
				}
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
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
