package ritzow.solomon.engine.network;

import java.util.Arrays;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.World;

public final class Protocol {
	
	/** The maximum length a sent message can be **/
	public static final short MAX_MESSAGE_LENGTH = 1024;
		
	/** The maximum number of milliseconds to wait for a response **/
	public static final short MAX_TIMEOUT = 1000;
	
	/** Client/server message protocol ID **/
	public static final short
		RESPONSE_MESSAGE = 1,
		SERVER_CONNECT_REQUEST = 2,
		SERVER_CONNECT_ACKNOWLEDGMENT = 3,
		CLIENT_INFO = 4,
		WORLD_HEAD = 9,
		WORLD = 10,
		CLIENT_DISCONNECT = 11;
	
	private static final short[] reliable = {
		2, 3, 4, 9, 10, 11, 100
	};
	
	static {
		Arrays.sort(reliable);
	}
	
	public static boolean isReliable(short protocol) {
		return Arrays.binarySearch(reliable, protocol) >= 0;
	}
	
	public static byte[] constructClientDisconnect(int messageID) {
		byte[] packet = new byte[6];
		ByteUtil.putInteger(packet, 0, messageID);
		ByteUtil.putShort(packet, 4, CLIENT_DISCONNECT);
		return packet;
	}
	
	public static World deconstructWorldPackets(byte[][] packets) {
		int bytes = 0;
		for(byte[] a : packets) {
			bytes += a.length;
		}
		byte[] concatenated = new byte[bytes];
		
		int index = 0;
		for(byte[] a : packets) {
			System.arraycopy(a, 0, concatenated, index, a.length);
			index += a.length;
		}
		try {
			return new World(ByteUtil.decompress(concatenated));
		} catch(ReflectiveOperationException e) {
			return null;
		}
	}
	
	public static byte[][] constructWorldPackets(int messageID, World world) {
		byte[] worldBytes = ByteUtil.compress(world.getBytes());
		int numWorldPackets = worldBytes.length/MAX_MESSAGE_LENGTH + (worldBytes.length % MAX_MESSAGE_LENGTH > 0 ? 1 : 0);
		byte[][] packets = new byte[numWorldPackets + 1][];
		
		byte[] head = new byte[8];
		ByteUtil.putInteger(head, 0, messageID);
		ByteUtil.putShort(head, 4, WORLD_HEAD);
		ByteUtil.putInteger(head, 8, numWorldPackets);
		packets[0] = head;
		
		for(short i = 1; i < packets.length; i++) {
			byte[] packet = new byte[i < packets.length - 1 ? MAX_MESSAGE_LENGTH : worldBytes.length % MAX_MESSAGE_LENGTH];
			ByteUtil.putInteger(packet, 0, messageID + i); //TODO this aint gonna work
			ByteUtil.putShort(packet, 4, WORLD);
			ByteUtil.putShort(packet, 6, i);
			System.arraycopy(worldBytes, i * MAX_MESSAGE_LENGTH, packet, 8, packet.length);
			packets[i] = packet;
		}
		return packets;
	}
	
	public static byte[] constructServerConnectRequest() {
		byte[] packet = new byte[6];
		ByteUtil.putInteger(packet, 0, 0);
		ByteUtil.putShort(packet, 4, SERVER_CONNECT_REQUEST);
		return packet;
	}
	
	public static byte[] constructServerConnectResponse(boolean accepted) {
		byte[] packet = new byte[7];
		ByteUtil.putInteger(packet, 0, 0);
		ByteUtil.putShort(packet, 4, SERVER_CONNECT_ACKNOWLEDGMENT);
		ByteUtil.putBoolean(packet, 6, accepted);
		return packet;
	}
	
	public static boolean deconstructServerConnectResponse(byte[] packet) {
		if(ByteUtil.getShort(packet, 4) != SERVER_CONNECT_ACKNOWLEDGMENT)
			throw new RuntimeException("incorrect message type");
		return ByteUtil.getBoolean(packet, 6);
	}
	
	public static byte[] constructMessageResponse(int messageID, int receivedMessageID) {
		byte[] packet = new byte[10];
		ByteUtil.putInteger(packet, 0, messageID);
		ByteUtil.putShort(packet, 4, RESPONSE_MESSAGE);
		ByteUtil.putInteger(packet, 6, receivedMessageID);
		return packet;
	}
}