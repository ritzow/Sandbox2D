package ritzow.solomon.engine.network.message.client;

import ritzow.solomon.engine.network.message.Message;

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

	@Override
	public boolean isReliable() {
		return true;
	}

}
