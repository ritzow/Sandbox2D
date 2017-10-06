package ritzow.sandbox.world;

import ritzow.sandbox.util.ByteUtil;
import ritzow.sandbox.util.DataReader;
import ritzow.sandbox.util.Serializer;
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
		for(int row = 0; row < height; row++) {
			for(int column = 0; column < width; column++) {
				blocks[row][column] = data.readObject();
			}
		}
	}
	
	@Override
	public byte[] getBytes(Serializer ser) {	
		int width = getWidth();
		int height = getHeight();
		
		byte[][] blockData = new byte[width * height][];
		int totalBytes = 0;
		
		for(int row = 0; row < height; row++) {
			for(int column = 0; column < width; column++) {
				byte[] serialized = ser.serialize(blocks[row][column]);
				blockData[row * width + column] = serialized;
				totalBytes += serialized.length;
			}
		}
		
		//width, height, data
		byte[] data = new byte[8 + totalBytes];
		ByteUtil.putInteger(data, 0, width);
		ByteUtil.putInteger(data, 4, height);
		
		int offset = 8;
		for(byte[] b : blockData) {
			ByteUtil.copy(b, data, offset);
			offset += b.length;
		}
		
		return data;
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
		return blocks[0].length;
	}
	
	public int getHeight() {
		return blocks.length;
	}
}
