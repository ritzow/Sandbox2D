package network.message;

import networkutils.ByteUtil;
import networkutils.InvalidMessageException;

public class ServerConnectRequest extends Message {
	
	public ServerConnectRequest() {
		
	}
	
	public ServerConnectRequest(byte[] packet) throws InvalidMessageException {
		if(packet.length < 2 || ByteUtil.getShort(packet, 0) != Protocol.SERVER_CONNECT_REQUEST)
			throw new InvalidMessageException();
	}

	@Override
	public byte[] getBytes() {
		byte[] message = new byte[2];
		ByteUtil.putShort(message, 0, Protocol.SERVER_CONNECT_REQUEST);
		return message;
	}

	@Override
	public String toString() {
		return "Server connect request";
	}

}
