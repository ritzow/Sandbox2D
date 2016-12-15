package world.entity;

import graphics.ModelRenderer;
import util.ByteUtil;

public class SerializationTestEntity extends Entity {
	
	protected int testInteger;
	protected float testFloat;
	
	public SerializationTestEntity(int testInt, float testFloat) {
		this.positionX = 52;
		this.positionY = 10002;
		this.velocityX = 100.9f;
		this.velocityY = 2.32f;
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
	public void render(ModelRenderer renderer) {
		//TODO implement method 'render'

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
	
	public String toString() {
		return super.toString() + ", testInteger = " + testInteger + ", testFloat = " + testFloat;
	}

}
