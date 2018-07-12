package ritzow.sandbox.network;

import java.nio.charset.Charset;
import ritzow.sandbox.data.Bytes;

public final class Protocol {
	
	private Protocol() {throw new UnsupportedOperationException("instantiation of Protocol not allowed");}
	
	/** The Charset for text encoding used by the client and server **/
	public static final Charset CHARSET = Charset.forName("UTF-8");
	
	public static final boolean COMPRESS_WORLD_DATA = false;
	
	public static final int 
			TIMEOUT_DISCONNECT = 1000,
			RESEND_COUNT = 10,
			RESEND_INTERVAL = TIMEOUT_DISCONNECT/RESEND_COUNT;
	
	public static final int DEFAULT_SERVER_UDP_PORT = 50000;
	
	/** The maximum length a sent message can be in bytes **/
	public static final short MAX_MESSAGE_LENGTH = Short.MAX_VALUE;
	
	/** Message Protocol ID **/
	public static final short
		CONSOLE_MESSAGE = 0,
		SERVER_CONNECT_ACKNOWLEDGMENT = 1,
		SERVER_WORLD_HEAD = 2,
		SERVER_WORLD_DATA = 3,
		SERVER_ENTITY_UPDATE = 4,
		SERVER_ADD_ENTITY = 5, //TODO separate receiving entity and adding to world
		SERVER_REMOVE_ENTITY = 6,  //TODO separate removing entity from world and deleting entity
		SERVER_CLIENT_DISCONNECT = 7,
		SERVER_PLAYER_ID = 8,
		SERVER_REMOVE_BLOCK = 9,
		CLIENT_CONNECT_REQUEST = 10,
		CLIENT_DISCONNECT = 11,
		CLIENT_PLAYER_ACTION = 12,
		CLIENT_BREAK_BLOCK = 13,
		PING = 14,
		SERVER_PLAYER_ACTION = 15,
		CLIENT_BOMB_THROW = 16,
		CLIENT_WORLD_BUILT = 17;
	
	/** Serialization Type ID **/
	public static final short
		WORLD = 1,
		BLOCK_GRID = 2,
		ITEM_ENTITY = 3,
		PARTICLE_ENTITY = 4,
		PLAYER_ENTITY = 5,
		BLOCK_ITEM = 6,
		INVENTORY = 7,
		DIRT_BLOCK = 8,
		GRASS_BLOCK = 9,
		RED_BLOCK = 10,
		BOMB_ENTITY = 11;
	
	public static byte[] buildConsoleMessage(String message) {
		byte[] msg = message.getBytes(Protocol.CHARSET);
		byte[] packet = new byte[2 + msg.length];
		Bytes.putShort(packet, 0, Protocol.CONSOLE_MESSAGE);
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