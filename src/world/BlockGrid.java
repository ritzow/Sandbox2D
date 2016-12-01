package world;

import java.io.Serializable;
import world.block.Block;

public class BlockGrid implements Serializable {
	private static final long serialVersionUID = -6699666931303107158L;

	protected final Block[][] blocks;
	
	protected World world;
	
	public BlockGrid(World world, int width, int height) {
		blocks = new Block[height][width];
		this.world = world;
	}
	
	public Block[][] toArray() {
		return toArray(0, 0, blocks.length, blocks[0].length);
	}
	
	/**
	 * 
	 * @param x the starting x-coordinate to copy, from the left
	 * @param y the starting y-coordinate to copy, from the bottom
	 * @param width the width of the rectangular area to copy
	 * @param height the height of the rectangular area to copy
	 * @return a new 2D block array representing the rectangle of blocks specified by the parameters
	 */ //TODO is this actually from the bottom left? it doesnt use the get and set methods
	public Block[][] toArray(int x, int y, int width, int height) {
		Block[][] range = new Block[height][width];
		for(int row = y; row < y + height; row++) {
			System.arraycopy(blocks[row], x, range[row - y], 0, width);
		}
		return range;
	}
	
	public boolean isValid(int x, int y) {
		return y < blocks.length && y >= 0 && x < blocks[y].length && x >= 0;
	}
	
	public Block get(int x, int y) {
		return blocks[blocks.length - 1 - y][x];
	}
	
	public synchronized void set(int x, int y, Block block) {
		blocks[blocks.length - 1 - y][x] = block;
	}
	
	public synchronized boolean destroy(int x, int y) {
		if(isBlock(x, y)) {
			get(x, y).onBreak(world, x, y);
			set(x, y, null);
			return true;
		} else {
			return false;
		}
	}
	
	public synchronized boolean place(int x, int y, Block block) {
		if(!isBlock(x, y) && isStable(x, y)) {
			set(x, y, block);
			block.onPlace(world, x, y);
			return true;
		} else {
			return false;
		}
	}
	
	/** Returns whether or not there is a block at the specified block coordinates **/
	public boolean isBlock(int x, int y) {
		return isValid(x, y) && get(x, y) != null;
	}
	
	/** Returns whether or not there is a block at the specified world coordinates **/
	public boolean isBlock(float worldX, float worldY) {
		return isBlock(Math.round(worldX), Math.round(worldY));
	}
	
	public boolean isHidden(int x, int y) {
		return  isBlock(x - 1, y) && isBlock(x + 1, y) && isBlock(x, y - 1) && isBlock(x, y + 1);
	}
	
	public boolean isFloating(int x, int y) {
		return !(isBlock(x - 1, y) || isBlock(x + 1, y) || isBlock(x, y - 1) || isBlock(x, y + 1));
	}
	
	public boolean isStable(int x, int y) { //TODO remove this method from block grid, too specific
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
