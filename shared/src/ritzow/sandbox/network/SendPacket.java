package ritzow.sandbox.network;

public final class SendPacket {
	public final byte[] data;
	public final int messageID;
	public final int lastReliableID;
	public final boolean reliable;
	public long lastSendTime, timeout;

	public SendPacket(byte[] data, int messageID, int lastReliableID, boolean reliable, long lastSendTime) {
		this.data = data;
		this.messageID = messageID;
		this.lastReliableID = lastReliableID;
		this.reliable = reliable;
		this.lastSendTime = lastSendTime;
		this.timeout = Protocol.RESEND_INTERVAL; //TODO need to have a "max resends" counter again to determine when to consider connection dropped
	}
}