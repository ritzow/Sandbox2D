package network.message;

import static networkutils.ByteUtil.putFloat;
import static networkutils.ByteUtil.putShort;

import networkutils.Message;
import world.entity.Entity;

public class EntityUpdateMessage extends Message {
	
	protected Entity entity;
	
	public EntityUpdateMessage(Entity entity) {
		this.entity = entity;
	}

	@Override
	public byte[] getBytes() {
		byte[] data = new byte[22];
		putShort(data, 0, getMessageID());
		putFloat(data, 2, entity.getPositionX());
		putFloat(data, 6, entity.getPositionY());
		putFloat(data, 10, entity.getVelocityX());
		putFloat(data, 14, entity.getVelocityY());
		return data;
	}
	
	public static short getMessageID() {
		return 32;
	}

	@Override
	public String toString() {
		return entity.toString();
	}

}
