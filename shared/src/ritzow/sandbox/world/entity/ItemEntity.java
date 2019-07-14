package ritzow.sandbox.world.entity;

import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.item.Item;

public class ItemEntity<I extends Item> extends Entity {
	protected final I item;
	
	public ItemEntity(int entityID, I item) {
		super(entityID);
		this.item = item;
	}
	
	public ItemEntity(int entityID, I item, float x, float y) {
		super(entityID);
		this.item = item;
		this.positionX = x;
		this.positionY = y;
	}
	
	public ItemEntity(TransportableDataReader input) {
		super(input);
		item = input.readObject();
	}
	
	@Override
	public byte[] getBytes(Serializer ser) {
		return Bytes.concatenate(super.getBytes(ser), ser.serialize(item));
	}
	
	public I getItem() {
		return item;
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
		return 1f;
	}
}
