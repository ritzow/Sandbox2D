package ritzow.sandbox.world.block;

import ritzow.sandbox.client.graphics.Models;

public class DirtBlock extends Block {
	
	public DirtBlock() {
		
	}
	
	public DirtBlock(byte[] data) {
		
	}

	@Override
	public int getModelIndex() {
		return Models.DIRT_INDEX;
	}

	@Override
	public int getHardness() {
		return 5;
	}

	@Override
	public float getFriction() {
		return 0.05f;
	}

	@Override
	public Block createNew() {
		return new DirtBlock();
	}

	@Override
	public String getName() {
		return "dirt";
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
