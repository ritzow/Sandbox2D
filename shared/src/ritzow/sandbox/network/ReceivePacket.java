package ritzow.sandbox.network;

public final record ReceivePacket(int messageID, int predecessorReliableID, boolean reliable, byte[] data) implements Comparable<ReceivePacket> {
	@Override
	public int compareTo(ReceivePacket o) {
		return Integer.compare(messageID, o.messageID);
	}
}
