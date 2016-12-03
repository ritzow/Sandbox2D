package network.message.server;

import static util.ByteUtil.getBoolean;
import static util.ByteUtil.getShort;
import static util.ByteUtil.putBoolean;
import static util.ByteUtil.putShort;

import java.net.DatagramPacket;
import network.message.InvalidMessageException;
import network.message.Message;
import network.message.Protocol;

public class ServerConnectAcknowledgment extends Message {
	
	protected boolean accepted;
	
	public ServerConnectAcknowledgment(boolean connectionAccepted) {
		this.accepted = connectionAccepted;
	}
	
	public ServerConnectAcknowledgment(DatagramPacket packet) throws InvalidMessageException {
		if(packet.getLength() < 3 || getShort(packet.getData(), packet.getOffset()) != Protocol.SERVER_CONNECT_ACKNOWLEDGMENT)
			throw new InvalidMessageException();
		this.accepted = getBoolean(packet.getData(), packet.getOffset() + 2);
	}
	
	@Override
	public byte[] getBytes() {
		byte[] message = new byte[3];
		putShort(message, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
		putBoolean(message, 2, accepted);
		return message;
	}

	@Override
	public String toString() {
		return "Server connection accepted: " + accepted;
	}

	public boolean isAccepted() {
		return accepted;
	}

	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}

}
