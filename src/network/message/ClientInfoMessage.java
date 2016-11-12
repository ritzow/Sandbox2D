package network.message;

import java.nio.charset.Charset;

import networkutils.InvalidMessageException;

import static networkutils.ByteUtil.*;

public class ClientInfoMessage extends Message {
	
	protected String username;
	
	public ClientInfoMessage(String username) {
		this.username = username;
	}
	
	public ClientInfoMessage(byte[] data) throws InvalidMessageException {
		byte length = data[2];
		this.username = new String(data, 3, length);
	}

	//format: protocol 2, 2 bytes, username length, 1 byte, username string, username length bytes
	@Override
	public byte[] getBytes() {
		byte[] username = this.username.getBytes(Charset.forName("UTF-8"));
		byte[] message = new byte[3 + username.length];
		putShort(message, 0, (short)2);
		message[2] = (byte)username.length;
		for(int i = 0; i < username.length; i++) {
			message[i + 3] = username[i];
		}
		return message;
	}

	@Override
	public String toString() {
		return "Username: " + username;
	}

}
