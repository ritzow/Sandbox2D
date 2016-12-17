package world.block;

import graphics.Model;
import resource.Models;

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
	public boolean doCollision() {
		return true;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {integrity};
	}

}
