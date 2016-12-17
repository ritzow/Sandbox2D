package world.block;

import graphics.Model;
import resource.Models;

public class RedBlock extends Block {
	
	public RedBlock() {
		
	}
	
	public RedBlock(byte[] data) {
		super(data);
	}

	@Override
	public Model getModel() {
		return Models.RED_SQUARE;
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
	public boolean doCollision() {
		return true;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {integrity};
	}
}
