package ritzow.sandbox.server.network;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;
import ritzow.sandbox.network.ReceivePacket;
import ritzow.sandbox.network.SendPacket;

public class ClientNetworkInfo {
	final InetSocketAddress address;
	final Queue<SendPacket> sendQueue;
	final Queue<ReceivePacket> receiveQueue;
	int sendMessageID = 0, lastSendReliableID = -1, headProcessedID = -1;
	long lastMessageProcessTime;

	/** Client reliable message round trip time in nanoseconds */
	long ping;

	ClientNetworkInfo(InetSocketAddress address) {
		this.address = address;
		sendQueue = new ArrayDeque<>();
		receiveQueue = new PriorityQueue<>();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ClientNetworkInfo client && address.equals(client.address);
	}

	public void send(byte[] data, boolean reliable) {
		sendQueue.add(new SendPacket(data, sendMessageID, lastSendReliableID, reliable, -1));
		if(reliable) lastSendReliableID = sendMessageID;
		sendMessageID++;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return "ClientNetworkInfo{" +
		   "address=" + address +
		   ", sendQueue=" + sendQueue +
		   ", receiveQueue=" + receiveQueue +
		   ", sendMessageID=" + sendMessageID +
		   ", lastSendReliableID=" + lastSendReliableID +
		   ", headProcessedID=" + headProcessedID +
		   ", lastMessageReceiveTime=" + lastMessageProcessTime +
		   ", ping=" + ping +
		   '}';
	}
}
