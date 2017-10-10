package ritzow.sandbox.world.block;

import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.world.World;

public abstract class Block implements Transportable {
	
	//package private empty array for blocks that dont need special serialization
	static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	
	public abstract int getHardness();
	public abstract float getFriction();
	public abstract boolean isSolid();
	public abstract Block createNew();
	public void onBreak(World world, float x, float y) {/* optional */}
	public void onPlace(World world, float x, float y) {/* optional */}
	public abstract String getName();
}
