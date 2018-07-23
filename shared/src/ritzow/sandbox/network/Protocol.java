package ritzow.sandbox.network;

import java.nio.charset.Charset;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.util.Utility;

public final class Protocol {
	
	private Protocol() {throw new UnsupportedOperationException("instantiation of Protocol not allowed");}
	
	/** The Charset for text encoding used by the client and server **/
	public static final Charset CHARSET = Charset.forName("UTF-8");
	
	public static final boolean COMPRESS_WORLD_DATA = true;
	
	public static final int 
		TIMEOUT_DISCONNECT = 1000,
		RESEND_COUNT = 10,
		RESEND_INTERVAL = TIMEOUT_DISCONNECT/RESEND_COUNT;
	
	public static final int DEFAULT_SERVER_UDP_PORT = 50000;
	public static final int STARTING_SEND_ID = 0;
	
	public static final int 
		HEADER_SIZE = 5, 
		MAX_PACKET_SIZE = 512,
		MAX_MESSAGE_LENGTH = MAX_PACKET_SIZE - HEADER_SIZE;
	
	public static final byte 
		RESPONSE_TYPE = 1, 
		RELIABLE_TYPE = 2, 
		UNRELIABLE_TYPE = 3;
	
	/** Message Protocol ID **/
	public static final short
		TYPE_CONSOLE_MESSAGE = 0,
		TYPE_SERVER_CONNECT_ACKNOWLEDGMENT = 1,
		TYPE_SERVER_WORLD_HEAD = 2,
		TYPE_SERVER_WORLD_DATA = 3,
		TYPE_SERVER_ENTITY_UPDATE = 4,
		TYPE_SERVER_ADD_ENTITY = 5, //TODO separate receiving entity and adding to world (TYPE_RECEIVE_AND_ADD_ENTITY vs TYPE_RECEIVE_ENTITY and TYPE_ADD_ENTITY?)
		TYPE_SERVER_REMOVE_ENTITY = 6,  //TODO separate removing entity from world and deleting entity
		TYPE_SERVER_CLIENT_DISCONNECT = 7,
		TYPE_SERVER_PLAYER_ID = 8,
		TYPE_SERVER_REMOVE_BLOCK = 9,
		TYPE_CLIENT_CONNECT_REQUEST = 10,
		TYPE_CLIENT_DISCONNECT = 11,
		TYPE_CLIENT_PLAYER_ACTION = 12,
		TYPE_CLIENT_BREAK_BLOCK = 13,
		TYPE_SERVER_PING = 14,
		TYPE_SERVER_PLAYER_ACTION = 15,
		TYPE_CLIENT_BOMB_THROW = 16,
		TYPE_CLIENT_WORLD_BUILT = 17;
	
	/** Serialization Type ID **/
	public static final short
		DATA_WORLD = 1,
		DATA_BLOCK_GRID = 2,
		DATA_ITEM_ENTITY = 3,
		DATA_PARTICLE_ENTITY = 4,
		DATA_PLAYER_ENTITY = 5,
		DATA_BLOCK_ITEM = 6,
		DATA_INVENTORY = 7,
		DATA_DIRT_BLOCK = 8,
		DATA_GRASS_BLOCK = 9,
		DATA_RED_BLOCK = 10,
		DATA_BOMB_ENTITY = 11;
	
	public static final byte 
		CONNECT_STATUS_REJECTED = 0, 
		CONNECT_STATUS_WORLD = 1, 
		CONNECT_STATUS_LOBBY = 2;
	
	public static final float BLOCK_BREAK_RANGE = 100;
	public static final long 
		BLOCK_BREAK_COOLDOWN_NANOSECONDS = Utility.millisToNanos(200),
		THROW_COOLDOWN_NANOSECONDS = Utility.millisToNanos(0);
	
	public static byte[] buildConsoleMessage(String message) {
		byte[] msg = message.getBytes(Protocol.CHARSET);
		byte[] packet = new byte[2 + msg.length];
		Bytes.putShort(packet, 0, Protocol.TYPE_CONSOLE_MESSAGE);
		Bytes.copy(msg, packet, 2);
		return packet;
	}
	
	public static enum PlayerAction {
		MOVE_LEFT(0),
		MOVE_RIGHT(1),
		MOVE_UP(2),
		MOVE_DOWN(3);
		
		private static final PlayerAction[] actions = PlayerAction.values();
		private final byte code;
		
		PlayerAction(int code) {
			if(code > Byte.MAX_VALUE || code < Byte.MIN_VALUE)
				throw new RuntimeException("invalid player action code");
			this.code = (byte)code;
		}
		
		public byte getCode() {
			return code;
		}
		
		public static PlayerAction forCode(byte code) {
			for(PlayerAction a : actions) {
				if(a.getCode() == code)
					return a;
			}
			throw new RuntimeException("unknown player action");
		}
	}
}