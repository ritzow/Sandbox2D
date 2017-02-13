package ritzow.solomon.engine.world.block;

import ritzow.solomon.engine.graphics.Model;
import ritzow.solomon.engine.resource.Models;

public class AirBlock extends Block {
	
	protected float density;
	
	public AirBlock() {
		density = 0.5f;
	}
	
	public AirBlock(byte[] data) {
		super(data);
	}
	
	@Override
	public byte[] getBytes() {
		return new byte[] {integrity};
	}

	@Override
	public String getName() {
		return "air";
	}

	@Override
	public Model getModel() {
		return Models.BLUE_SQUARE; //for testing
	}

	@Override
	public int getHardness() {
		return 0;
	}

	@Override
	public float getFriction() {
		return density;
	}

	@Override
	public Block createNew() {
		return new AirBlock();
	}

	@Override
	public boolean isSolid() {
		return false;
	}
}
