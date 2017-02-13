package ritzow.solomon.engine.world.block;

import ritzow.solomon.engine.graphics.Model;
import ritzow.solomon.engine.resource.Models;

public class DirtBlock extends Block {
	
	public DirtBlock() {
		
	}
	
	public DirtBlock(byte[] data) {
		super(data);
	}

	@Override
	public Model getModel() {
		return Models.DIRT_MODEL;
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
		return new byte[] {integrity};
	}

}
