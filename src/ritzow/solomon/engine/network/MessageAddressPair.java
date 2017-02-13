package ritzow.solomon.engine.network;

import java.net.SocketAddress;

final class MessageAddressPair {
	protected final SocketAddress recipient;
	protected final int messageID;
	protected volatile boolean received;
	
	public MessageAddressPair(SocketAddress recipient, int messageID) {
		this.messageID = messageID;
		this.recipient = recipient;
	}
}
