package ritzow.sandbox.world;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import ritzow.sandbox.util.ByteUtil;
import ritzow.sandbox.util.DataReader;
import ritzow.sandbox.util.Transportable;
import ritzow.sandbox.world.block.Block;

public final class BlockGrid implements Transportable {
	private final Block[][] blocks;
	
	public BlockGrid(int width, int height) {
		blocks = new Block[height][width];
	}
	
	public BlockGrid(DataReader data) {
		int width = data.readInteger();
		int height = data.readInteger();
		blocks = new Block[height][width];
		for(int row = 0; row < blocks.length; row++) {
			for(int column = 0; column < blocks[row].length; column++) {
				blocks[row][column] = data.readObject();
			}
		}
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
	
	@Override
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
		return y >= 0 && x >= 0 && y < blocks.length && x < blocks[y].length;
	}
	
	public Block get(int x, int y) {
		synchronized(blocks) {
			return blocks[blocks.length - 1 - y][x];
		}
	}
	
	public Block get(float worldX, float worldY) {
		return get(Math.round(worldX), Math.round(worldY));
	}
	
	public void set(int x, int y, Block block) {
		synchronized(blocks) {
			blocks[blocks.length - 1 - y][x] = block;
		}
	}
	
	public boolean destroy(World world, int x, int y) {
		if(isBlock(x, y)) {
			get(x, y).onBreak(world, x, y);
			set(x, y, null);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean place(World world, int x, int y, Block block) {
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
