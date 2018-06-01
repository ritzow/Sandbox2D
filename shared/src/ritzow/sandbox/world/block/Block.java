package ritzow.sandbox.world.block;

import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;

public interface Block extends Transportable {
	
	/** @return the decceleration applied to an entity that comes in contact with the block */
	float getFriction();
	
	/** @return whether or not entities can pass through the block */
	boolean isSolid();
	
	/** @return a human readable name for the Block type */
	String getName();
	
	/**
	 * Called when a block is broken
	 * @param world the World that contained the block
	 * @param grid the BlockGrid that contained the block
	 * @param x the x position at which the block was broken
	 * @param y the y position at which the block was broken
	 */
	default void onBreak(World world, BlockGrid grid, float x, float y) {/* optional */}
	/**
	 * Called when a block is placed
	 * @param world the World that contains the block
	 * @param grid the BlockGrid that contains the block
	 * @param x the x position at which the block was placed
	 * @param y the y position at which the block was placed
	 */
	default void onPlace(World world, BlockGrid grid, float x, float y) {/* optional */}
}