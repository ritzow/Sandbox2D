package ritzow.sandbox.client.world.block;

import static ritzow.sandbox.util.Utility.randomFloat;

import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;

public abstract class ClientBlock extends Block {
	@Override
	public abstract String getName();
	public abstract int getModelIndex();
	
	@Override
	public void onBreak(World world, BlockGrid grid, float x, float y) {
		world.getAudioSystem().playSound(Sound.BLOCK_BREAK.code(), x, y, 0, 0, randomFloat(0.75f, 1.5f), randomFloat(0.75f, 1.5f));
	}

	@Override
	public void onPlace(World world, BlockGrid grid, float x, float y) {
		world.getAudioSystem().playSound(Sound.BLOCK_PLACE.code(), x, y, 0, 0, 1, randomFloat(0.9f, 1.1f));
	}

}
