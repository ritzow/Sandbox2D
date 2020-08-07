package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.audio.Sound.StandardSound;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.graphics.Light;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;

public interface ClientBlockProperties extends Block {
	Model getModel();
	boolean isTransparent();

	default Light lighting() {
		return null;
	}

	@Override
	default void onBreak(World world, float x, float y) {
		throw new UnsupportedOperationException("client does not support default onBreak");
	}

	@Override
	default void onPlace(World world, float x, float y) {
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
	default void onBreak(World world, BlockGrid grid, Camera camera, float x, float y) {
		AudioSystem.getDefault()
		.playSound(StandardSound.BLOCK_BREAK, x, y, 0, 0,
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
	default void onPlace(World world, BlockGrid grid, Camera camera, float x, float y) {
		AudioSystem.getDefault()
			.playSound(StandardSound.BLOCK_PLACE, x, y, 0, 0, camera.getZoom() * 2, Utility.random(0.9f, 1.1f));
	}
}
