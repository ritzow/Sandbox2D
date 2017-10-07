package ritzow.sandbox.server.world.entity;

import ritzow.sandbox.util.DataReader;
import ritzow.sandbox.world.Entity;

public class PlayerEntity extends Entity {
	
	public PlayerEntity(DataReader reader) {
		super(reader);
	}

	public PlayerEntity(int entityID) {
		super(entityID);
	}
	
	public void setLeft(boolean left) {
	}

	public void setRight(boolean right) {
	}

	public void setUp(boolean up) {
	}

	public void setDown(boolean down) {
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
