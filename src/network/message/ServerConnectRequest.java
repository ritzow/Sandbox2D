package network.message;

import networkutils.ByteUtil;

public class ServerConnectRequest extends Message {

	@Override
	public byte[] getBytes() {
		byte[] message = new byte[2];
		ByteUtil.putShort(message, 0, (short)1);
		return message;
	}

	@Override
	public String toString() {
		return "Server connect request";
	}

}
