package ritzow.solomon.engine.network;

import java.nio.charset.Charset;
import java.util.Arrays;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.entity.Entity;
import ritzow.solomon.engine.world.entity.PlayerEntity;

public final class Protocol {
	
	/** The maximum length a sent message can be in bytes **/
	public static final short MAX_MESSAGE_LENGTH = 1000;
	
	/** Server message protocol ID **/
	public static final short
		SERVER_CONNECT_ACKNOWLEDGMENT = 2,
		SERVER_WORLD_HEAD = 4,
		SERVER_WORLD_DATA = 5,
		SERVER_PLAYER_ENTITY_COMPRESSED = 8,
		SERVER_ENTITY_LOCATION = 9,
		SERVER_ENTITY_UPDATE = 11,
		SERVER_ADD_ENTITY = 12,
		SERVER_ADD_ENTITY_COMPRESSED = 13,
		SERVER_REMOVE_ENTITY = 14,
		SERVER_CLIENT_DISCONNECT = 15;
	
	/** Client message protocol ID **/
	public static final short
		CLIENT_CONNECT_REQUEST = 1,
		CLIENT_INFO = 3,
		CLIENT_DISCONNECT = 6,
		CLIENT_PLAYER_ACTION = 7;
	
	/** Shared protocol ID **/
	public static final short
		CONSOLE_MESSAGE = 10;
	
	private static final short[] RELIABLE_PROTOCOLS = {
		CLIENT_CONNECT_REQUEST,
		CLIENT_DISCONNECT,
		CLIENT_PLAYER_ACTION,
		CLIENT_INFO,
		SERVER_CONNECT_ACKNOWLEDGMENT,
		SERVER_WORLD_HEAD,
		SERVER_WORLD_DATA,
		SERVER_PLAYER_ENTITY_COMPRESSED,
		SERVER_ADD_ENTITY,
		SERVER_REMOVE_ENTITY,
		SERVER_CLIENT_DISCONNECT,
		CONSOLE_MESSAGE
	};
	
	static {
		Arrays.sort(RELIABLE_PROTOCOLS);
		short previous = RELIABLE_PROTOCOLS[0];
		for(int i = 1; i < RELIABLE_PROTOCOLS.length; i++) {
			if(RELIABLE_PROTOCOLS[i] == previous) {
				System.err.println("Two or more protocols with the same ID");
				System.exit(1);
			}
		}
	}
	
	public static boolean isReliable(short protocol) {
		for(short p : RELIABLE_PROTOCOLS) {
			if(p == protocol) {
				return true;
			}
		}
		return false;
	}
	
	public static byte[] buildServerDisconnect() {
		byte[] packet = new byte[2];
		ByteUtil.putShort(packet, 0, SERVER_CLIENT_DISCONNECT);
		return packet;
	}
	
	public static byte[] buildConsoleMessage(String message) {
		byte[] msg = message.getBytes(Charset.forName("UTF-8"));
		byte[] packet = new byte[2 + msg.length];
		ByteUtil.putShort(packet, 0, Protocol.CONSOLE_MESSAGE);
		ByteUtil.copy(msg, packet, 2);
		return packet;
	}
	
	public static byte[] buildGenericEntityUpdate(Entity e) {
		//protocol, id, posX, posY, velX, velY
		byte[] update = new byte[2 + 4 + 4 + 4 + 4 + 4];
		ByteUtil.putShort(update, 0, SERVER_ENTITY_UPDATE);
		ByteUtil.putInteger(update, 2, e.getID());
		ByteUtil.putFloat(update, 6, e.getPositionX());
		ByteUtil.putFloat(update, 10, e.getPositionY());
		ByteUtil.putFloat(update, 14, e.getVelocityX());
		ByteUtil.putFloat(update, 18, e.getVelocityY());
		return update;
	}
	
	public static byte[] buildGenericEntityRemoval(int entityID) {
		byte[] packet = new byte[2 + 4];
		ByteUtil.putShort(packet, 0, SERVER_REMOVE_ENTITY);
		ByteUtil.putInteger(packet, 2, entityID);
		return packet;
	}
	
	public static byte[] buildEntityTransfer(Entity e, boolean compressed) {
		byte[] serialized = compressed ? ByteUtil.compress(ByteUtil.serialize(e)) : ByteUtil.serialize(e);
		byte[] packet = new byte[2 + serialized.length];
		ByteUtil.putShort(packet, 0, compressed ? SERVER_ADD_ENTITY_COMPRESSED : SERVER_ADD_ENTITY);
		ByteUtil.copy(serialized, packet, 2);
		return packet;
	}
	
	public static class PlayerAction {
		public static final byte
			PLAYER_LEFT = 0,
			PLAYER_RIGHT = 1,
			PLAYER_UP = 2,
			PLAYER_DOWN = 3;
		
		public static final byte[] buildPlayerMovementAction(byte action, boolean enable) {
			byte[] packet = new byte[4];
			ByteUtil.putShort(packet, 0, CLIENT_PLAYER_ACTION);
			packet[2] = action;
			ByteUtil.putBoolean(packet, 3, enable);
			return packet;
		}
		
		public static final void processPlayerMovementAction(byte[] data, PlayerEntity player) {
			byte action = data[2];
			boolean enable = ByteUtil.getBoolean(data, 3);
			switch(action) {
			case PLAYER_LEFT:
				player.setLeft(enable);
				break;
			case PLAYER_RIGHT:
				player.setRight(enable);
				break;
			case PLAYER_UP:
				player.setUp(enable);
				break;
			case PLAYER_DOWN:
				player.setDown(enable);
				break;
			}
		}
	}
	
	protected static byte[][] constructWorldPackets(World world) {
		int headerSize = 2;
		int dataBytesPerPacket = Protocol.MAX_MESSAGE_LENGTH - headerSize;

		//serialize the world for transfer, no need to use ByteUtil.serialize because we know what we're serializing (until I start subclassing world)
		byte[] worldBytes = ByteUtil.compress(ByteUtil.serialize(world));
		
		//split world data into evenly sized packets and one extra packet if not evenly divisible by max packet size
		int packetCount = (worldBytes.length/dataBytesPerPacket) + (worldBytes.length % dataBytesPerPacket > 0 ? 1 : 0);
		
		//create the array to store all the constructed packets to send, in order
		byte[][] packets = new byte[1 + packetCount][];

		//create the first packet to send, which contains the number of subsequent packets
		byte[] head = new byte[6];
		ByteUtil.putShort(head, 0, Protocol.SERVER_WORLD_HEAD);
		ByteUtil.putInteger(head, 2, packetCount);
		packets[0] = head;
		
		//construct the packets containing the world data, which begin with a standard header and contain chunks of world bytes
		for(int slot = 1, index = 0; slot < packets.length; slot++) {
			int remaining = worldBytes.length - index;
			int dataSize = Math.min(dataBytesPerPacket, remaining);
			byte[] packet = new byte[headerSize + dataSize];
			ByteUtil.putShort(packet, 0, Protocol.SERVER_WORLD_DATA);
			System.arraycopy(worldBytes, index, packet, headerSize, dataSize);
			packets[slot] = packet;
			index += dataSize;
		}
		
		return packets;
	}
	
	protected static World reconstructWorld(byte[][] data) {
		final int headerSize = 2; //WORLD_DATA protocol
		//create a sum of the number of bytes so that a single byte array can be allocated
		int bytes = 0;
		for(byte[] a : data) {
			bytes += (a.length - headerSize);
		}
		
		//the size of the world data (sum of all arrays without 2 byte headers)
		byte[] worldBytes = new byte[bytes];
		
		//copy the received packets into the final world data array
		int index = 0;
		for(byte[] array : data) {
			System.arraycopy(array, headerSize, worldBytes, index, array.length - headerSize);
			index += (array.length - headerSize);
		}
		
		try {
			return (World)ByteUtil.deserialize(ByteUtil.decompress(worldBytes));
		} catch(ReflectiveOperationException e) {
			e.printStackTrace();
			return null;
		}
	}
}