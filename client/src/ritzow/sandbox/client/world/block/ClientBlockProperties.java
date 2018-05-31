package ritzow.sandbox.client.world.block;

import static ritzow.sandbox.util.Utility.randomFloat;

import ritzow.sandbox.client.audio.DefaultAudioSystem;
import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;

public interface ClientBlockProperties {
	int getModelIndex();
	boolean isTransparent();
	
	public default void onBreak(World world, BlockGrid grid, float x, float y) {
		DefaultAudioSystem.getDefault()
			.playSound(Sound.BLOCK_BREAK.code(), x, y, 0, 0, randomFloat(0.75f, 1.5f), randomFloat(0.75f, 1.5f));
	}

	public default void onPlace(World world, BlockGrid grid, float x, float y) {
		DefaultAudioSystem.getDefault()
			.playSound(Sound.BLOCK_PLACE.code(), x, y, 0, 0, 1, randomFloat(0.9f, 1.1f));
	}
}
