package ritzow.solomon.engine.network.message;

import ritzow.solomon.engine.util.ByteUtil;

public class MessageResponse implements Message {
	
	protected final short protocol;
	protected final int id;

	public MessageResponse(int id, short protocol) {
		this.id = id;
		this.protocol = protocol;
	}
	
	public MessageResponse(byte[] data) {
		id = ByteUtil.getInteger(data, 0);
		protocol = ByteUtil.getShort(data, 4);
	}

	@Override
	public byte[] getBytes() {
		byte[] data = new byte[6];
		ByteUtil.putInteger(data, 0, id);
		ByteUtil.putShort(data, 4, protocol);
		return data;
	}
	
	public short getProtocol() {
		return protocol;
	}
	
	public int getMessageID() {
		return id;
	}

	@Override
	public boolean isReliable() {
		return false; //if it was reliable it would cause an infinite loop
	}

}
