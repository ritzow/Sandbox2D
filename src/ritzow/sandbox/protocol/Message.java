package ritzow.sandbox.protocol;

import java.net.SocketAddress;

public class Message extends ByteArrayInput {
	private final SocketAddress sender;

	public Message(SocketAddress sender, byte[] data) {
		super(data);
		this.sender = sender;
	}
	
	public SocketAddress getSender() {
		return sender;
	}


}
