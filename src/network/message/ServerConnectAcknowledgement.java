package network.message;

import static networkutils.ByteUtil.*;

public class ServerConnectAcknowledgement extends Message {
	
	protected boolean accepted;
	
	public ServerConnectAcknowledgement(boolean connectionAccepted) {
		this.accepted = connectionAccepted;
	}
	
	public ServerConnectAcknowledgement(byte[] packet) {
		this.accepted = getBoolean(packet, 2);
	}

	@Override
	public byte[] getBytes() {
		byte[] message = new byte[3];
		putShort(message, 0, (short)50);
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
