package network.message;

import networkutils.ByteUtil;
import networkutils.Message;

public class ServerInfoRequest extends Message {

	@Override
	public byte[] getBytes() {
		byte[] message = new byte[2];
		ByteUtil.putShort(message, 0, (short)5);
		return message;
	}

	@Override
	public String toString() {
		return "Server info request";
	}

}
