package ritzow.solomon.engine.network;

import java.net.SocketAddress;

final class MessageAddressPair {
	private final int messageID;
	private final SocketAddress recipient;
	private volatile boolean received;
	
	public MessageAddressPair(int messageID, SocketAddress recipient) {
		this.messageID = messageID;
		this.recipient = recipient;
		this.received = false;
	}
	
	public int getMessageID() {
		return messageID;
	}
	
	public SocketAddress getRecipient() {
		return recipient;
	}
	
	public boolean getReceived() {
		return received;
	}
	
	public void setReceived() {
		received = true;
	}
}
