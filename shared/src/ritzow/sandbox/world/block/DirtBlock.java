package ritzow.sandbox.world.block;

import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Serializer;

public class DirtBlock implements Block {

	public static final DirtBlock INSTANCE = new DirtBlock();

	protected DirtBlock() {}

	@Override
	public float getFriction() {
		return 0.5f;
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
		return Bytes.EMPTY_BYTE_ARRAY;
	}
}
