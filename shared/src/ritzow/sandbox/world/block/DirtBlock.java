package ritzow.sandbox.world.block;

import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.data.Serializer;

public class DirtBlock implements Block {
	
	public DirtBlock() {
		
	}
	
	public DirtBlock(DataReader data) {
		
	}

	@Override
	public float getFriction() { //TODO this seems to have no effect
		return 0.05f;
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
	public byte[] getBytes(Serializer ser) {
		return ByteUtil.EMPTY_BYTE_ARRAY;
	}
}
