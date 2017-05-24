package ritzow.sandbox.world.block;

import ritzow.sandbox.client.graphics.Models;

public class RedBlock extends Block {
	
	public RedBlock() {
		
	}
	
	public RedBlock(byte[] data) {
		
	}
	
	@Override
	public int getModelIndex() {
		return Models.RED_SQUARE_INDEX;
	}

	@Override
	public int getHardness() {
		return 10;
	}

	@Override
	public float getFriction() {
		return 0.01f;
	}

	@Override
	public Block createNew() {
		return new RedBlock();
	}

	@Override
	public String getName() {
		return "Red";
	}

	@Override
	public boolean isSolid() {
		return true;
	}

	@Override
	public byte[] getBytes() {
		return new byte[0];
	}
}
