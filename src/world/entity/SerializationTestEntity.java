package world.entity;

import util.ByteUtil;

public class SerializationTestEntity extends Entity {
	
	protected int testInteger;
	protected float testFloat;
	
	public SerializationTestEntity(float x, float y, float vx, float vy, int testInt, float testFloat) {
		this.positionX = x;
		this.positionY = y;
		this.velocityX = vx;
		this.velocityY = vy;
		this.testInteger = testInt;
		this.testFloat = testFloat;
	}
	
	public SerializationTestEntity(byte[] data) {
		super(data);
		this.testInteger = ByteUtil.getInteger(data, 16);
		this.testFloat = ByteUtil.getFloat(data, 20);
	}

	@Override
	public byte[] toBytes() {
		byte[] sup = super.toBytes();
		byte[] data = new byte[sup.length + 8];
		System.arraycopy(sup, 0, data, 0, sup.length);
		ByteUtil.putInteger(data, sup.length, testInteger);
		ByteUtil.putFloat(data, sup.length + 4, testFloat);
		return data;
	}

	@Override
	public boolean getShouldDelete() {
		return false;
	}

	@Override
	public boolean doCollision() {
		return false;
	}

	@Override
	public boolean doBlockCollisionResolution() {
		return false;
	}

	@Override
	public boolean doEntityCollisionResolution() {
		return false;
	}

	@Override
	public float getFriction() {
		return 0;
	}

	@Override
	public float getWidth() {
		return 0;
	}

	@Override
	public float getHeight() {
		return 0;
	}

	@Override
	public float getMass() {
		return 0;
	}
	
	public String toString() {
		return super.toString() + ", testInteger = " + testInteger + ", testFloat = " + testFloat;
	}

}
