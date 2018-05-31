package ritzow.sandbox.world.block;

import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.data.Serializer;

public class GrassBlock extends Block {
	
	public GrassBlock() {
		
	}
	
	public GrassBlock(DataReader input) {
		
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
	public String getName() {
		return "Grass";
	}

	@Override
	public boolean isSolid() {
		return true;
	}
	
	@Override
	public byte[] getBytes(Serializer ser) {
		return ByteUtil.EMPTY_BYTE_ARRAY;
	}
}
