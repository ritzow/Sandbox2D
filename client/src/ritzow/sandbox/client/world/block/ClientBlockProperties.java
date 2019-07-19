package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.audio.DefaultAudioSystem;
import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;

public interface ClientBlockProperties extends Block {
	int getModelIndex();
	boolean isTransparent();
	
	public default void onBreak(World world, BlockGrid grid, float x, float y) {
		throw new UnsupportedOperationException("client does not support default onBreak");
	}

	public default void onPlace(World world, BlockGrid grid, float x, float y) {
		throw new UnsupportedOperationException("client does not support default onPlace");
	}
	
	public default void onBreak(World world, BlockGrid grid, Camera camera, float x, float y) {
		DefaultAudioSystem.getDefault()
		.playSound(Sound.BLOCK_BREAK.code(), x, y, 0, 0, 
				camera.getZoom() * 2 * Utility.random(0.75f, 1.5f), Utility.random(0.75f, 1.5f));
	}
	
	public default void onPlace(World world, BlockGrid grid, Camera camera, float x, float y) {
		DefaultAudioSystem.getDefault()
			.playSound(Sound.BLOCK_PLACE.code(), x, y, 0, 0, camera.getZoom() * 2, Utility.random(0.9f, 1.1f));
	}
}
