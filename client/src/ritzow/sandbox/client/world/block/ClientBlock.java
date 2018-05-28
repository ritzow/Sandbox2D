package ritzow.sandbox.client.world.block;

import static ritzow.sandbox.util.Utility.randomFloat;

import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.client.audio.ClientAudioSystem;

public abstract class ClientBlock extends Block {
	
	@Override
	public abstract String getName();
	public abstract int getModelIndex();
	public abstract boolean isTransparent();
	
	@Override
	public void onBreak(World world, BlockGrid grid, float x, float y) {
		ClientAudioSystem.getAudioSystem()
			.playSound(Sound.BLOCK_BREAK.code(), x, y, 0, 0, randomFloat(0.75f, 1.5f), randomFloat(0.75f, 1.5f));
	}

	@Override
	public void onPlace(World world, BlockGrid grid, float x, float y) {
		ClientAudioSystem.getAudioSystem()
			.playSound(Sound.BLOCK_PLACE.code(), x, y, 0, 0, 1, randomFloat(0.9f, 1.1f));
	}

}
