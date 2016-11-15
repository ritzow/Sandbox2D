package network.message;

import static networkutils.ByteUtil.putFloat;
import static networkutils.ByteUtil.putShort;

import world.entity.Entity;

public class EntityUpdateMessage extends Message {
	
	protected Entity entity;
	
	public EntityUpdateMessage(Entity entity) {
		this.entity = entity;
	}
	
	public EntityUpdateMessage(byte[] packet) {
		
	}

	@Override
	public byte[] getBytes() {
		byte[] data = new byte[22];
		putShort(data, 0, Protocol.ENTITY_UPDATE);
		putFloat(data, 2, entity.getPositionX());
		putFloat(data, 6, entity.getPositionY());
		putFloat(data, 10, entity.getVelocityX());
		putFloat(data, 14, entity.getVelocityY());
		return data;
	}

	@Override
	public String toString() {
		return entity.toString();
	}

}
