package network.message;

import java.net.DatagramPacket;
import util.ByteUtil;

public class ServerConnectRequest extends Message {
	
	public ServerConnectRequest() {
		
	}
	
	public ServerConnectRequest(DatagramPacket packet) throws InvalidMessageException {
		if(packet.getLength() < 2 || ByteUtil.getShort(packet.getData(), packet.getOffset()) != Protocol.SERVER_CONNECT_REQUEST)
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
