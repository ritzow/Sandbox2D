package ritzow.sandbox.world.block;

import ritzow.sandbox.data.Serializer;

public class GlassBlock implements Block {
	public static final GlassBlock INSTANCE = new GlassBlock();

	protected GlassBlock() {}

	@Override
	public float getFriction() {
		return 0;
	}

	@Override
	public boolean isSolid() {
		return true;
	}

	@Override
	public String getName() {
		return "glass";
	}

	@Override
	public byte[] getBytes(Serializer ser) {
		return new byte[0];
	}
}
