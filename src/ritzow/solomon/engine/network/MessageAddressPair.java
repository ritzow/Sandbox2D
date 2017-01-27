package ritzow.solomon.engine.network;

import java.net.SocketAddress;

public final class MessageAddressPair {
	private final int messageID;
	private final SocketAddress recipient;
	private boolean received;
	
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
	
	public synchronized boolean getReceived() {
		return received;
	}
	
	public synchronized void setReceived() {
		received = true;
	}
}
