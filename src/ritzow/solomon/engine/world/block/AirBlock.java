package ritzow.solomon.engine.world.block;

import ritzow.solomon.engine.graphics.Model;
import ritzow.solomon.engine.resource.Models;
import ritzow.solomon.engine.world.World;

public class AirBlock extends Block {
	
	public AirBlock() {
		
	}
	
	public AirBlock(byte[] data) {
		super(data);
	}
	
	@Override
	public byte[] getBytes() {
		return new byte[] {integrity};
	}

	//TODO should I implement air density? friction value changes based on current air density/player suffocates when air density is too low
	@Override
	public String getName() {
		return "air";
	}

	@Override
	public Model getModel() {
		return Models.BLUE_SQUARE;
	}

	@Override
	public int getHardness() {
		return 0;
	}

	@Override
	public float getFriction() {
		return 0.5f;
	}
	
	@Override
	public void onBreak(World world, float x, float y) {
		
	}

	@Override
	public void onPlace(World world, float x, float y) {
		
	}

	@Override
	public Block createNew() {
		return new AirBlock();
	}

	@Override
	public boolean doCollision() {
		return false;
	}
}
