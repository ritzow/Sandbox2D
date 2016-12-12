package network.message.server;

import static util.ByteUtil.getBoolean;
import static util.ByteUtil.putBoolean;

import network.message.InvalidMessageException;
import network.message.Message;

public class ServerConnectAcknowledgment implements Message {
	
	protected boolean accepted;
	
	public ServerConnectAcknowledgment(boolean connectionAccepted) {
		this.accepted = connectionAccepted;
	}
	
	public ServerConnectAcknowledgment(byte[] data) throws InvalidMessageException {
		this.accepted = getBoolean(data, 0);
	}
	
	public ServerConnectAcknowledgment(byte[] data, int offset) throws InvalidMessageException {
		this.accepted = getBoolean(data, offset);
	}

	@Override
	public byte[] getBytes() {
		byte[] message = new byte[1];
		putBoolean(message, 0, accepted);
		return message;
	}

	@Override
	public String toString() {
		return "Server connection accepted: " + accepted;
	}

	public boolean isAccepted() {
		return accepted;
	}

}
