package world;

import java.io.Serializable;
import world.block.Block;

public class BlockGrid implements Serializable {
	private static final long serialVersionUID = 1L;

	protected final Block[][] blocks;
	
	/** A reference to the containing World object **/
	protected transient World world;
	
	//TODO refactor BlockGrid classes to have as little usage of outside classes as possible?
	public BlockGrid(World world, int width, int height) {
		blocks = new Block[height][width];
		this.world = world;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append('┌');
		for(int i = 1; i < blocks[0].length - 1; i++) {
			builder.append('─');
		}
		builder.append('┐');
		builder.append('\n');
		
		for(int i = 0; i < blocks.length; i++) {
			builder.append('│');
			for(int j = 1; j < blocks[i].length - 1; j++) {
				Block block = blocks[i][j];
				if(block != null) {
					builder.append(blocks[i][j].toString().charAt(0));
				} else {
					builder.append(" ");
				}
			}
			builder.append('│');
			builder.append('\n');
		}
		
		builder.append('└');
		for(int i = 1; i < blocks[0].length - 1; i++) {
			builder.append('─');
		}
		builder.append('┘');
		
		return builder.toString();
	}
	
	public boolean isValid(int x, int y) {
		return y < blocks.length && y >= 0 && x < blocks[y].length && x >= 0;
	}
	
	public Block get(int x, int y) {
		return blocks[blocks.length - 1 - y][x];
	}
	
	public Block get(float worldX, float worldY) {
		return get(Math.round(worldX), Math.round(worldY));
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
		if(!isBlock(x, y)) {
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
	
	public boolean isSurrounded(int x, int y) {
		return 	isBlock(x - 1, y) &&
				isBlock(x + 1, y) &&
				isBlock(x, y - 1) &&
				isBlock(x, y + 1);
	}
	
	public boolean isFloating(int x, int y) {
		return !(isBlock(x - 1, y) || isBlock(x + 1, y) || isBlock(x, y - 1) || isBlock(x, y + 1));
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
