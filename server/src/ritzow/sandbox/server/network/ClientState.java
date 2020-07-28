package ritzow.sandbox.server.network;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Queue;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.server.world.entity.ServerPlayerEntity;

public class ClientState extends ClientNetworkInfo {
	/** after the client acks the connect ack */
	public static final byte
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

	byte status;
	String disconnectReason;
	Queue<byte[]> recordedSend;

	/** Last player action times in nanoseconds offset */
	long lastPlayerStateUpdate;
	Instant nextUseTime;
	ServerPlayerEntity player;

	ClientState(InetSocketAddress address) {
		super(address);
		status = STATUS_CONNECTED;
		recordedSend = new ArrayDeque<>();
		nextUseTime = Instant.EPOCH;
	}

	public void sendRecorded() {
		while(!recordedSend.isEmpty()) {
			var val =recordedSend.poll();
			System.out.println(Bytes.getShort(val, 0));
			send(val, true);
		}
		recordedSend = null;
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

	boolean hasPending() {
		return !sendQueue.isEmpty();
	}

	public String formattedName() {
		return NetworkUtility.formatAddress(address) + " (" + statusToString(status) + ')';
	}
}
