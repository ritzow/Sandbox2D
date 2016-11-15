package network.message;

import networkutils.ByteUtil;

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
