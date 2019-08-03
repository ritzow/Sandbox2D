package ritzow.sandbox.world.block;

import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Serializer;

public class GrassBlock implements Block {
	
	public static final GrassBlock INSTANCE = new GrassBlock();
	
	protected GrassBlock() {}
	
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
		return Bytes.EMPTY_BYTE_ARRAY;
	}
}
