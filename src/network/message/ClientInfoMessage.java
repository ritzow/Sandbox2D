package network.message;

import java.nio.charset.Charset;
import networkutils.Message;

import static networkutils.ByteUtil.*;

public class ClientInfoMessage extends Message {
	
	protected String username;
	
	public ClientInfoMessage(String username) {
		this.username = username;
	}

	@Override
	public byte[] getBytes() {
		byte[] username = this.username.getBytes(Charset.forName("UTF-8"));
		byte[] message = new byte[2 + username.length];
		putShort(message, 0, (short)3);
		for(int i = 0; i < username.length; i++) {
			message[i] = username[i]; //fill the first bytes after the message type of the message with the username
		}
		
		return message;
	}

}
