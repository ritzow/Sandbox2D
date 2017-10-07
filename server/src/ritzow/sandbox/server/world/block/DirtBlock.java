package ritzow.sandbox.server.world.block;

import ritzow.sandbox.util.DataReader;
import ritzow.sandbox.util.Serializer;
import ritzow.sandbox.world.Block;
import ritzow.sandbox.world.World;

public class DirtBlock extends Block {
	
	public DirtBlock(DataReader data) {
		
	}
	
	public DirtBlock() {
		
	}

	@Override
	public byte[] getBytes(Serializer ser) {
		//TODO implement method 'getBytes'
		return null;
	}

	@Override
	public String getName() {
		//TODO implement method 'getName'
		return null;
	}

	@Override
	public int getModelIndex() {
		//TODO implement method 'getModelIndex'
		return 0;
	}

	@Override
	public int getHardness() {
		//TODO implement method 'getHardness'
		return 0;
	}

	@Override
	public float getFriction() {
		//TODO implement method 'getFriction'
		return 0;
	}

	@Override
	public boolean isSolid() {
		//TODO implement method 'isSolid'
		return false;
	}

	@Override
	public Block createNew() {
		//TODO implement method 'createNew'
		return null;
	}

	@Override
	public void onBreak(World world, float x, float y) {
		//TODO implement method 'onBreak'

	}

	@Override
	public void onPlace(World world, float x, float y) {
		//TODO implement method 'onPlace'

	}

}
