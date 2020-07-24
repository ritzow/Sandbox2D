package ritzow.sandbox.world;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.block.Block;

public final class BlockGrid implements Transportable {
	private final Block[] blocks;
	private final int width, height, layers;

	public BlockGrid(int layers, int width, int height) {
		blocks = new Block[layers * width * height];
		this.width = width;
		this.layers = layers;
		this.height = height;
	}

	public BlockGrid(TransportableDataReader data) {
		//instance fields are evaluated inthe order they appear in the constructor
		this.width = data.readInteger();
		this.height = data.readInteger();
		this.layers = data.readInteger();
		blocks = new Block[layers * width * height];
		for(int i = 0; i < blocks.length; i++) {
			blocks[i] = data.readObject();
		}
	}

	@Override
	public byte[] getBytes(Serializer ser) {
		//Store all serialized block data
		byte[][] blockData = new byte[blocks.length][];
		for(int i = 0; i < blocks.length; i++) {
			blockData[i] = ser.serialize(blocks[i]);
		}
		byte[] data = Bytes.concatenate(12, blockData);
		Bytes.putInteger(data, 0, getWidth());
		Bytes.putInteger(data, 4, getHeight());
		Bytes.putInteger(data, 8, layers);
		return data;
	}

	@Override
	public String toString() {
		int width = getWidth();
		int height = getHeight();
		String border = "─".repeat(width);
		StringBuilder builder = new StringBuilder(layers * (width + 2) * (height + 2));
		for(int layer = 0; layer < layers; layer++) {
			builder.append('┌').append(border).append('┐').append('\n');
			for(int row = height - 1; row >= 0; --row) {
				builder.append('│');
				for(int column = 0; column < width; ++column) {
					Block block = get(layer, column, row);
					System.out.println(column + " " + row + " " + compute(layer, column, row));
					builder.append(block == null ? " " : Character.toUpperCase(block.getName().charAt(0)));
				}
				builder.append('│').append('\n');
			}
			builder.append('└').append(border).append('┘').append('\n');
		}
		return builder.toString();
	}

	public boolean isValid(int layer, int x, int y) {
		return layer < layers && y >= 0 && x >= 0 && y < getHeight() && x < getWidth();
	}

	public boolean isValidAndBlock(int layer, int x, int y) {
		return isValid(layer, x, y) && blocks[compute(layer, x, y)] != null;
	}

	/**
	 * Returns the block at the provided block coordinates
	 * @param x the distance from the bottom of the world, in blocks
	 * @param y the distance from the left of the world, in blocks
	 * @return true if there is a block at x, y
	 * @throws IllegalArgumentException if x or y is not in the block grid
	 */
	public Block get(int layer, int x, int y) {
		try {
			return blocks[compute(checkLayer(layer), x, y)];
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Invalid coordinates layer "
				+ layer + ", pos ("  + x + ", " + y + ") in world sized "
				+ getWidth() + " X " + getHeight() + " X " + layers
				+ " layers, element " + compute(layer, x, y) + "/" + blocks.length);
		}
	}

	private int checkLayer(int layer) {
		if(layer >= layers)
			throw new IllegalArgumentException("invalid layer " + layer + ", maximum is " + (layers - 1));
		return layer;
	}

	public Block get(int layer, float worldX, float worldY) {
		return get(layer, Math.round(worldX), Math.round(worldY));
	}

	public Block set(int layer, int x, int y, Block block) {
		try {
			int index = compute(checkLayer(layer), x, y);
			Block previous = blocks[index];
			blocks[index] = block;
			return previous;
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("invalid coordinates " + x + ", " + y);
		}
	}

	/** Determines the memory layout of the block grid dimensions **/
	private int compute(int layer, int x, int y) {
		//Want layout to be row -> column ->
		//return width * layers * y + x * layers;
		//get start of row: width * layers * y
		//get offset into a single row: layers * x + layer
		//combined: (width * layers * y) + (layers * x + layer)
		//optimized:
		return layers * (width * y + x) + layer;
	}

	/** Fills all layers of a rectangular region with the provided block instance **/
	public void fill(Block block, int x1, int y1, int width, int height) {
		int y2 = y1 + height;
		int columnStart = x1 * layers;
		int columnEnd = (x1 + width) * layers;
		for(int row = y1; row < y2; ++row) {
			int rowStart = this.width * layers * row;
			Arrays.fill(blocks, rowStart + columnStart, rowStart + columnEnd, block);
		}
	}

	public Iterator<Block> blockIterator(int layerLast, int layerFirst, int x, int y, int width, int height) {
		return new Iterator<Block>() { //TODO test this to see that it works
			int current;
			final int last;

			{

				last = (layerLast - layerFirst) * (width * height) - 1;
					//compute(layerLast, x + width, y + height);
			}

			@Override
			public boolean hasNext() {
				return current < last;
			}

			@Override
			public Block next() {
				//current = width * y + x + layer;
				//x = current - layer - width * y
				throw new UnsupportedOperationException("not implemented");
				//return null; //blocks[]; //TODO implement blockIterator
			}
		};
	}

	public Block destroy(World world, int layer, float x, float y) {
		return destroy(world, layer, Math.round(x), Math.round(y));
	}

	public Block destroy(World world, int layer, int x, int y) {
		Block prev = set(layer, x, y, null);
		if(prev != null)
			prev.onBreak(world, this, x, y);
		return prev;
	}

	public boolean place(World world, int layer, float x, float y, Block block) {
		return place(world, layer, Math.round(x), Math.round(y), block);
	}

	public boolean place(World world, int layer, int x, int y, Block block) {
		if(!isBlock(layer, x, y)) {
			set(layer, x, y, Objects.requireNonNull(block));
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
	public boolean isBlock(int layer, int x, int y) {
		return get(layer, x, y) != null;
	}

	/** Returns whether or not there is a block at the specified world coordinates
	 * @param worldX The world x coordinate to check.
	 * @param worldY The world y coordinate to check.
	 * @return true if there is a block at the specified coordinates. **/
	public boolean isBlock(int layer, float worldX, float worldY) {
		return isBlock(layer, Math.round(worldX), Math.round(worldY));
	}

	public boolean isSolidBlockAdjacent(int layer, int blockX, int blockY) {
		return 	(isValidAndBlock(layer, blockX + 1, blockY)) ||
				(isValidAndBlock(layer, blockX - 1, blockY)) ||
				(isValidAndBlock(layer, blockX, blockY + 1)) ||
				(isValidAndBlock(layer, blockX, blockY - 1));
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getLayers() {
		return layers;
	}
}
