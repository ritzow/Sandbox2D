package ritzow.sandbox.network;

public final class SendPacket {
	public final byte[] data;
	public final int messageID;
	public final int lastReliableID;
	public final boolean reliable;
	public long lastSendTime;

	public SendPacket(byte[] data, int messageID, int lastReliableID, boolean reliable, long lastSendTime) {
		this.data = data;
		this.messageID = messageID;
		this.lastReliableID = lastReliableID;
		this.reliable = reliable;
		this.lastSendTime = lastSendTime;
	}
}