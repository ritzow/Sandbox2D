package ritzow.sandbox.world;

import java.util.Arrays;
import java.util.Objects;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.block.Block;

public final class BlockGrid implements Transportable {
	private final Block[] blocks;
	private final int width;

	public BlockGrid(int width, int height) {
		blocks = new Block[width * height];
		this.width = width;
	}

	public BlockGrid(TransportableDataReader data) {
		int width = data.readInteger();
		int height = data.readInteger();
		this.width = width;
		blocks = new Block[width * height];
		for(int row = 0; row < height; row++) {
			for(int column = 0; column < width; column++) {
				set(column, row, data.readObject());
				//blocks[row][column] = data.readObject();
			}
		}
		data.readInteger(); //TODO figure out why this extra read is necessary?
		data.readInteger(); //TODO figure out why this extra read is necessary?
	}

	@Override
	public byte[] getBytes(Serializer ser) {
		int width = getWidth();
		int height = getHeight();

		byte[][] blockData = new byte[width * height][];

		for(int row = 0; row < height; row++) {
			for(int column = 0; column < width; column++) {
				blockData[row * width + column] = ser.serialize(get(column, row));
			}
		}

		byte[] data = Bytes.concatenate(8, blockData);
		Bytes.putInteger(data, 0, width);
		Bytes.putInteger(data, 4, height);
		return data;
	}

	@Override
	public String toString() {
		int width = getWidth();
		int height = getHeight();
		String border = "─".repeat(width);
		StringBuilder builder = new StringBuilder(width * height)
				.append('┌').append(border).append('┐').append('\n');
		for(int row = height - 1; row >= 0; --row) {
			builder.append('│');
			for(int column = 0; column < width; ++column) {
				Block block = get(column, row);
				builder.append(block == null ? " " : Character.toUpperCase(block.getName().charAt(0)));
			}
			builder.append('│').append('\n');
		}
		return builder.append('└').append(border).append('┘').toString();
	}

	public boolean isValid(int x, int y) {
		return y >= 0 && x >= 0 && y < getHeight() && x < getWidth();
	}

	/**
	 * Returns the block at the provided block coordinates
	 * @param x the distance from the bottom of the world, in blocks
	 * @param y the distance from the left of the world, in blocks
	 * @return true if there is a block at x, y
	 * @throws IllegalArgumentException if x or y is not in the block grid
	 */
	public Block get(int x, int y) {
		try {
			//return blocks[blocks.length - 1 - y][x];
			return blocks[width * y + x];
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("invalid coordinates " + x + ", " + y);
		}
	}

	public Block get(float worldX, float worldY) {
		return get(Math.round(worldX), Math.round(worldY));
	}

	public Block set(int x, int y, Block block) {
		try {
			Block previous = blocks[width * y + x];
			blocks[width * y + x] = block;
			return previous;
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("invalid coordinates " + x + ", " + y);
		}
	}
	
	public void fill(Block block, int x1, int y1, int width, int height) {
		int x2 = x1 + width;
		int y2 = y1 + height;
		for(int row = y1; row < y2; ++row) {
			int rowStart = this.width * row;
			Arrays.fill(blocks, rowStart + x1, rowStart + x2, block);
		}
	}

	public Block destroy(World world, float x, float y) {
		return destroy(world, Math.round(x), Math.round(y));
	}

	public Block destroy(World world, int x, int y) {
		Block prev = set(x, y, null);
		if(prev != null)
			prev.onBreak(world, this, x, y);
		return prev;
	}

	public boolean place(World world, float x, float y, Block block) {
		return place(world, Math.round(x), Math.round(y), block);
	}

	public boolean place(World world, int x, int y, Block block) {
		if(!isBlock(x, y)) {
			set(x, y, Objects.requireNonNull(block));
			block.onPlace(world, this, x, y);
			return true;
		}
		return false;
	}

	//TODO make every cell non-null (air blocks) and remove this method
	/**
	 * @param x the horizontal block coordinate to check
	 * @param y the vertical block coordinate to check
	 * @return whether or not there is a block at the specified block coordinates
	 */
	public boolean isBlock(int x, int y) {
		return get(x, y) != null;
	}

	/** Returns whether or not there is a block at the specified world coordinates **/
	public boolean isBlock(float worldX, float worldY) {
		return isBlock(Math.round(worldX), Math.round(worldY));
	}
	
	public boolean isSolidBlockAdjacent(int blockX, int blockY) {
		return 	(isValid(blockX + 1, blockY) && isBlock(blockX + 1, blockY)) ||
				(isValid(blockX - 1, blockY) && isBlock(blockX - 1, blockY)) ||
				(isValid(blockX, blockY + 1) && isBlock(blockX, blockY + 1)) ||
				(isValid(blockX, blockY - 1) && isBlock(blockX, blockY - 1));
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return blocks.length/width;
	}
}
