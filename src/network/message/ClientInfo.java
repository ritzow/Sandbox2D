package network.message;

import static util.ByteUtil.*;

import java.net.DatagramPacket;
import java.nio.charset.Charset;

public class ClientInfo extends Message {
	
	protected String username;
	
	public ClientInfo(String username) {
		this.username = username;
	}
	
	public ClientInfo(DatagramPacket packet) throws InvalidMessageException {
		if(packet.getLength() < 2 || getShort(packet.getData(), 0) != Protocol.CLIENT_INFO)
			throw new InvalidMessageException();
		byte length = packet.getData()[packet.getOffset() + 2];
		this.username = new String(packet.getData(), packet.getOffset() + 3, length);
	}

	//format: protocol 2, 2 bytes, username length, 1 byte, username string, username length bytes
	@Override
	public byte[] getBytes() {
		byte[] username = this.username.getBytes(Charset.forName("UTF-8"));
		byte[] message = new byte[3 + username.length];
		putShort(message, 0, Protocol.CLIENT_INFO);
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
