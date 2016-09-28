package world;

import world.block.Block;

public class BlockGrid  {
	protected final Block[][] blocks;
	
	protected World world;
	
	public BlockGrid(World world, int width, int height) {
		blocks = new Block[height][width];
		this.world = world;
	}
	
	public Block get(int x, int y) {
		try {
			return blocks[blocks.length - 1 - y][x];
		} catch(ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public void set(int x, int y, Block block) {
		try {
			blocks[blocks.length - 1 - y][x] = block;
		} catch(ArrayIndexOutOfBoundsException e) {
			
		}
	}
	
	public void destroy(int x, int y) {
		if(get(x, y) != null) {
			get(x, y).onBreak(world, x, y);
			set(x, y, null);
		}
	}
	
	public void place(int x, int y, Block block) {
		if(get(x, y) == null && isStable(x, y)) {
			set(x, y, block);
			block.onPlace(world, x, y);
		}
	}
	
	/** Returns whether or not there is a block at the specified block coordinates **/
	public boolean isBlock(int x, int y) {
		return get(x, y) != null;
	}
	
	/** Returns whether or not there is a block at the specified world coordinates **/
	public boolean isBlock(float worldX, float worldY) {
		return isBlock(Math.round(worldX), Math.round(worldY));
	}
	
	public boolean isHidden(int x, int y) {	
		return isBlock(x - 1, y) && isBlock(x + 1, y) && isBlock(x, y - 1) && isBlock(x, y + 1);
	}
	
	public boolean isFloating(int x, int y) {
		return !(isBlock(x - 1, y) || isBlock(x + 1, y) || isBlock(x, y - 1) || isBlock(x, y + 1));
	}
	
	public boolean isStable(int x, int y) {
		return (isBlock(x, y - 1) || isBlock(x - 1, y) || isBlock(x + 1, y));
	}
	
	public int getWidth() {
		if(blocks.length > 0) {
			return blocks[0].length;
		}
		
		else {
			return 0;
		}
	}
	
	public int getHeight() {
		return blocks.length;
	}
	
	public int getTotalBlocks() {
		return getWidth() * getHeight();
	}
}
