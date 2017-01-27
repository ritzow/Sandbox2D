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
		WORLD_DATA = 10,
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
	
	public static World deconstructWorldPackets(byte[][] data) {
		int headerSize = 2;
		
		int bytes = 0;
		for(byte[] a : data) {
			bytes += (a.length - headerSize);
		}
		
		byte[] concatenated = new byte[bytes]; //the number of bytes in all the arrays combined without the header packet id
		
		int index = 0;
		short packet = 1;
		while(packet < data.length) {
			for(byte[] a : data) {
				int length = a.length - headerSize;
				if(ByteUtil.getShort(a, 0) == packet) {
					System.arraycopy(a, headerSize, concatenated, index, length);
					index += length;
					packet++;
				}
			}
		}
		
		try {
			return new World(ByteUtil.decompress(concatenated));
		} catch(ReflectiveOperationException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static byte[][] constructWorldPackets(int headMessageID, World world) {
		//serialize the world for transfer, no need to use ByteUtil.serialize because we know what we're serializing (until I start subclassing world)
		byte[] worldBytes = ByteUtil.compress(world.getBytes());
		
		//split world data into evenly sized packets and one extra packet if not evenly divisible by max packet size
		int packetCount = worldBytes.length/MAX_MESSAGE_LENGTH + (worldBytes.length % MAX_MESSAGE_LENGTH > 0 ? 1 : 0);
		
		//create the array to store all the constructed packets to send, in order
		byte[][] packets = new byte[packetCount + 1][];

		//create the first packet to send, which contains the number of subsequent packets
		byte[] head = new byte[12];
		ByteUtil.putInteger(head, 0, headMessageID);
		ByteUtil.putShort(head, 4, WORLD_HEAD);
		ByteUtil.putInteger(head, 6, packetCount);
		packets[0] = head;
		
		//construct the packets containing the world data, which begin with a standard header and contain chunks of world bytes
		int index = 0;
		for(short i = 1; i < packets.length; i++) {
			int headerSize = 8;
			int dataSize = Math.min(MAX_MESSAGE_LENGTH - headerSize, worldBytes.length - index);
			byte[] packet = new byte[headerSize + dataSize];
			ByteUtil.putInteger(packet, 0, headMessageID + i); //TODO change to use smarter message ids (perhaps require an input of valid ids?)
			ByteUtil.putShort(packet, 4, WORLD_DATA); //insert protocol id
			ByteUtil.putShort(packet, 6, i);
			System.arraycopy(worldBytes, index, packet, headerSize, dataSize);
			packets[i] = packet;
			index += dataSize;
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