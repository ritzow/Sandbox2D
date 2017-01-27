package ritzow.solomon.engine.world;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.util.Transportable;
import ritzow.solomon.engine.world.block.Block;

public class BlockGrid implements Transportable {
	protected final Block[][] blocks;
	
	public BlockGrid(World world, int width, int height) {
		blocks = new Block[height][width];
	}
	
	public BlockGrid(byte[] data) {
		blocks = new Block[ByteUtil.getInteger(data, 4)][ByteUtil.getInteger(data, 0)];
		int index = 8;
		for(int row = 0; row < blocks.length; row++) {
			for(int column = 0; column < blocks[row].length; column++) {
				try {
					blocks[row][column] = (Block)ByteUtil.deserialize(data, index);
					index += ByteUtil.getSerializedLength(data, index);
				} catch (ReflectiveOperationException e) {
					blocks[row][column] = null;
					e.printStackTrace(); //for now?
					continue;
				}
			}
		}
	}
	
	@Override
	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(out);
		try {
			dout.writeInt(blocks[0].length); //write the width 
			dout.writeInt(blocks.length); //write the height
			for(int row = 0; row < blocks.length; row++) {
				for(int column = 0; column < blocks[row].length; column++) {
					dout.write(ByteUtil.serialize(blocks[row][column]));
				}
			}
		} catch (IOException e) {
			return null;
		}
		return out.toByteArray();
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
	
	public synchronized boolean destroy(World world, int x, int y) {
		if(isBlock(x, y)) {
			get(x, y).onBreak(world, x, y);
			set(x, y, null);
			return true;
		} else {
			return false;
		}
	}
	
	public synchronized boolean place(World world, int x, int y, Block block) {
		if(!isBlock(x, y)) {
			set(x, y, block);
			block.onPlace(world, x, y);
			return true;
		} else {
			return false;
		}
	}
	
	/** 
	 * @param x the horizontal block coordinate to check
	 * @param y the vertical block coordinate to check
	 * @return whether or not there is a block at the specified block coordinates
	 */
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
