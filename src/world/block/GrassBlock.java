package world.block;

import graphics.Model;
import resource.Models;

public class GrassBlock extends Block {
	
	public GrassBlock() {
		
	}
	
	public GrassBlock(byte[] data) {
		super(data);
	}

	@Override
	public Model getModel() {
		return Models.GRASS_MODEL;
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
	public boolean doCollision() {
		return true;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {integrity};
	}
}
