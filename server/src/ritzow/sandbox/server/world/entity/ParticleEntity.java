package ritzow.sandbox.server.world.entity;

import ritzow.sandbox.util.DataReader;
import ritzow.sandbox.world.Entity;

public class ParticleEntity extends Entity {

	public ParticleEntity(DataReader reader) {
		super(reader);
	}

	@Override
	public boolean getShouldDelete() {
		//TODO implement method 'getShouldDelete'
		return false;
	}

	@Override
	public boolean doCollision() {
		//TODO implement method 'doCollision'
		return false;
	}

	@Override
	public boolean doBlockCollisionResolution() {
		//TODO implement method 'doBlockCollisionResolution'
		return false;
	}

	@Override
	public boolean doEntityCollisionResolution() {
		//TODO implement method 'doEntityCollisionResolution'
		return false;
	}

	@Override
	public float getFriction() {
		//TODO implement method 'getFriction'
		return 0;
	}

	@Override
	public float getWidth() {
		//TODO implement method 'getWidth'
		return 0;
	}

	@Override
	public float getHeight() {
		//TODO implement method 'getHeight'
		return 0;
	}

	@Override
	public float getMass() {
		//TODO implement method 'getMass'
		return 0;
	}

}
