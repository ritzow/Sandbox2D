package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;

public interface ClientBlockProperties extends Block {
	int getModelIndex();
	boolean isTransparent();
	
	@Override
	public default void onBreak(World world, BlockGrid grid, float x, float y) {
		throw new UnsupportedOperationException("client does not support default onBreak");
	}

	@Override
	public default void onPlace(World world, BlockGrid grid, float x, float y) {
		throw new UnsupportedOperationException("client does not support default onPlace");
	}
	
	/**
	 * Should be called when a block is broken in the world.
	 * @param world the world that the {@code grid} belongs to.
	 * @param grid the {@link BlockGrid} that the block belongs to.
	 * @param camera the player's camera.
	 * @param x the world x position the block was at.
	 * @param y the world y position the block was at.
	 */
	public default void onBreak(World world, BlockGrid grid, Camera camera, float x, float y) {
		AudioSystem.getDefault()
		.playSound(Sound.BLOCK_BREAK.code(), x, y, 0, 0, 
				camera.getZoom() * 2 * Utility.random(0.75f, 1.5f), Utility.random(0.75f, 1.5f));
	}
	
	/**
	 * Should be called when a block is placed in the world.
	 * @param world the world that the {@code grid} belongs to.
	 * @param grid the {@link BlockGrid} that the block belongs to.
	 * @param camera the player's camera.
	 * @param x the world x position the block was placed at.
	 * @param y the world y position the block was placed at.
	 */
	public default void onPlace(World world, BlockGrid grid, Camera camera, float x, float y) {
		AudioSystem.getDefault()
			.playSound(Sound.BLOCK_PLACE.code(), x, y, 0, 0, camera.getZoom() * 2, Utility.random(0.9f, 1.1f));
	}
}
