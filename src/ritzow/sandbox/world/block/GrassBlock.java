package ritzow.sandbox.world.block;

import ritzow.sandbox.client.graphics.Models;

public class GrassBlock extends Block {
	
	public GrassBlock() {
		
	}
	
	public GrassBlock(byte[] data) {
		
	}

	@Override
	public int getModelIndex() {
		return Models.GRASS_INDEX;
	}

	@Override
	public int getHardness() {
		return 5;
	}

	@Override
	public float getFriction() {
		return 0.04f;
	}

	@Override
	public Block createNew() {
		return new GrassBlock();
	}

	@Override
	public String getName() {
		return "Grass";
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