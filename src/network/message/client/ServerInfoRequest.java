package network.message.client;

import network.message.Message;
import network.message.Protocol;
import util.ByteUtil;

public class ServerInfoRequest extends Message {

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

}
