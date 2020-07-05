package ritzow.sandbox.server.network;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.ReceivePacket;
import ritzow.sandbox.network.SendPacket;
import ritzow.sandbox.server.world.entity.ServerPlayerEntity;
import ritzow.sandbox.util.Utility;

final class ClientState {
		/** after the client acks the connect ack */
	static final byte
		STATUS_CONNECTED = 0,
		/** if the client doesn't send an ack within ack interval */
		STATUS_TIMED_OUT = 1,
		/** if server kicks a player or shuts down */
		STATUS_KICKED = 2,
		/** if the client manually disconnects */
		STATUS_LEAVE = 3,
		/** if the server rejects the client */
		STATUS_REJECTED = 4,
		/** if the client has received the world and player and notifies server */
		STATUS_IN_GAME = 5,
		/** if the client sent data that didn't make sense */
		STATUS_INVALID = 6;

	int sendMessageID = 0, lastSendReliableID = -1, headProcessedID = -1;
	long lastMessageReceiveTime;
	final InetSocketAddress address;
	final Queue<SendPacket> sendQueue;
	final PriorityQueue<ReceivePacket> receiveQueue;
	byte status;
	String disconnectReason;

	/** Client reliable message round trip time in nanoseconds */
	long ping;

	/** Last player action times in nanoseconds offset */
	long lastBlockBreakTime, lastBlockPlaceTime, lastPlayerStateUpdate;
	ServerPlayerEntity player;

	ClientState(InetSocketAddress address) {
		this.address = address;
		status = STATUS_CONNECTED;
		sendQueue = new ArrayDeque<>();
		receiveQueue = new PriorityQueue<>();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ClientState client && address.equals(client.address);
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

	static String statusToString(byte status) {
		return switch(status) {
			case STATUS_CONNECTED -> 	"connected";
			case STATUS_TIMED_OUT -> 	"timed out";
			case STATUS_KICKED -> 		"kicked";
			case STATUS_LEAVE -> 		"leave";
			case STATUS_REJECTED -> 	"rejected";
			case STATUS_IN_GAME -> 		"in-game";
			case STATUS_INVALID ->		"invalid";
			default -> 					"unknown";
		};
	}

	boolean inGame() {
		return status == STATUS_IN_GAME;
	}

	boolean isNotDisconnectState() {
		return switch (status) {
			case STATUS_CONNECTED, STATUS_IN_GAME -> true;
			default -> false;
		};
	}

	boolean hasPending() {
		return !sendQueue.isEmpty();
	}

	public String formattedName() {
		return NetworkUtility.formatAddress(address) + " (" + statusToString(status) + ')';
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("ClientState[address: ")
			.append(NetworkUtility.formatAddress(address))
			.append(" send: ")
			.append(sendMessageID)
			.append(" headProcessedID: ")
			.append(headProcessedID)
			.append(" ping: ")
			.append(Utility.formatTime(ping))
			.append(" queuedSend: ")
			.append(sendQueue.size())
			.append(" ")
			.append(statusToString(status))
			.append(']')
			.toString();
	}
}
