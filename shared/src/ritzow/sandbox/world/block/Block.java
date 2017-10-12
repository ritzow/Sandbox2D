package ritzow.sandbox.world.block;

import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;

public abstract class Block implements Transportable {
	public abstract int getHardness();
	public abstract float getFriction();
	public abstract boolean isSolid();
	/**
	 * Called by BlockGrid when a block is broken
	 * @param world the World the block is part of
	 * @param grid the BlockGrid the block was part of
	 * @param x the x position of the block was broken at
	 * @param y the y position the block was broken at
	 */
	public void onBreak(World world, BlockGrid grid, float x, float y) {/* optional */}
	/**
	 * Called by BlockGrid when a block is placed
	 * @param world the World the block is part of
	 * @param grid the BlockGrid the block is part of
	 * @param x the x position of the block was placed at
	 * @param y the y position the block was placed at
	 */
	public void onPlace(World world, BlockGrid grid, float x, float y) {/* optional */}
	
	/**
	 * Returns a human readable name for the Block type
	 * @return a human readable name for the Block type
	 */
	public abstract String getName();
}
