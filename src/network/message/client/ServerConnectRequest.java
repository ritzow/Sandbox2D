package network.message.client;

import network.message.Message;

public class ServerConnectRequest implements Message {
	
	public ServerConnectRequest() {
		
	}

	@Override
	public byte[] getBytes() {
		return new byte[0];
	}

	@Override
	public String toString() {
		return "Server connect request";
	}

}
