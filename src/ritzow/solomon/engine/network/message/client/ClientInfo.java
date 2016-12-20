package ritzow.solomon.engine.network.message.client;

import java.nio.charset.Charset;
import ritzow.solomon.engine.network.message.InvalidMessageException;
import ritzow.solomon.engine.network.message.Message;

public class ClientInfo implements Message {
	
	protected String username;
	
	public ClientInfo(String username) {
		this.username = username;
	}
	
	public ClientInfo(byte[] data) throws InvalidMessageException {
		byte length = data[0];
		this.username = new String(data, 1, length);
	}

	@Override
	public byte[] getBytes() {
		byte[] nameBytes = username.getBytes(Charset.forName("UTF-8"));
		byte[] message = new byte[1 + nameBytes.length];
		System.arraycopy(nameBytes, 0, message, 1, nameBytes.length);
		return message;
	}

	@Override
	public String toString() {
		return "Username: " + username;
	}

	@Override
	public boolean isReliable() {
		return true;
	}

}
