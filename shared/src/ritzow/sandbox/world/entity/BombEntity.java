package ritzow.sandbox.world.entity;

import ritzow.sandbox.data.DataReader;

public class BombEntity extends Entity {

	public BombEntity(int entityID) {
		super(entityID);
	}

	public BombEntity(DataReader reader) {
		super(reader);
	}

	@Override
	public boolean getShouldDelete() {
		return false;
	}

	@Override
	public boolean hasCollision() {
		return false;
	}

	@Override
	public boolean collidesWithBlocks() {
		return true;
	}

	@Override
	public boolean collidesWithEntities() {
		return false;
	}

	@Override
	public float getFriction() {
		return 0.02f;
	}

	@Override
	public float getWidth() {
		return 0.5f;
	}

	@Override
	public float getHeight() {
		return 0.5f;
	}

	@Override
	public float getMass() {
		return 0;
	}

}
