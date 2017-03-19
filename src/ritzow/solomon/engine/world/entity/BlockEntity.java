package ritzow.solomon.engine.world.entity;

import ritzow.solomon.engine.world.block.Block;

public final class BlockEntity extends Entity {
	
	//private final Block block;
	
	public BlockEntity(byte[] data) {
		super(data);
		//this.block = null;
	}

	public BlockEntity(int entityID, Block block) {
		super(entityID);
		//this.block = block;
	}

	@Override
	public boolean getShouldDelete() {
		return false;
	}

	@Override
	public boolean doCollision() {
		return true;
	}

	@Override
	public boolean doBlockCollisionResolution() {
		return true;
	}

	@Override
	public boolean doEntityCollisionResolution() {
		return true;
	}

	@Override
	public float getFriction() {
		return 0.2f; //TODO adjust friction value
	}

	@Override
	public float getWidth() {
		return 1.0f;
	}

	@Override
	public float getHeight() {
		return 1.0f;
	}

	@Override
	public float getMass() {
		return 1.0f; //TODO adjust mass, based on block or something
	}
}
