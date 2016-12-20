package ritzow.solomon.engine.network.message.client;

import ritzow.solomon.engine.network.message.Message;
import ritzow.solomon.engine.network.message.Protocol;
import ritzow.solomon.engine.util.ByteUtil;

public class ServerInfoRequest implements Message {

	@Override
	public byte[] getBytes() {
		byte[] message = new byte[2];
		ByteUtil.putShort(message, 0, Protocol.SERVER_INFO_REQUEST);
		return message;
	}

	@Override
	public String toString() {
		return "Server info request";
	}

	@Override
	public boolean isReliable() {
		return true;
	}

}
