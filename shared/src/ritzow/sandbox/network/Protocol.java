package ritzow.sandbox.network;

import java.nio.charset.Charset;
import java.util.Set;
import ritzow.sandbox.data.ByteUtil;

public final class Protocol {
	
	private Protocol() {}
	
	/** The Charset used by the client and server **/
	public static final Charset CHARSET = Charset.forName("UTF-8");
	
	/** The maximum length a sent message can be in bytes **/
	public static final short MAX_MESSAGE_LENGTH = 1000;
	
	/** message protocol ID **/
	public static final short
		CONSOLE_MESSAGE = 0,
		SERVER_CONNECT_ACKNOWLEDGMENT = 1,
		SERVER_WORLD_HEAD = 2,
		SERVER_WORLD_DATA = 3,
		SERVER_ENTITY_UPDATE = 4,
		SERVER_ADD_ENTITY = 5,
		SERVER_REMOVE_ENTITY = 6,
		SERVER_CLIENT_DISCONNECT = 7,
		SERVER_PLAYER_ID = 8,
		SERVER_REMOVE_BLOCK = 9,
		CLIENT_CONNECT_REQUEST = 10,
		CLIENT_DISCONNECT = 11,
		CLIENT_PLAYER_ACTION = 12,
		CLIENT_BREAK_BLOCK = 13;
	
	/** serialization type ID **/
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
		RED_BLOCK = 10;
	
	private static final Set<Short> RELIABLE_PROTOCOLS = Set.of(
			CLIENT_CONNECT_REQUEST, //also SERVER_CONNECT_ACKNOWLEDGMENT
			CLIENT_DISCONNECT, //also SERVER_WORLD_DATA
			CLIENT_PLAYER_ACTION,
			SERVER_ADD_ENTITY,
			SERVER_REMOVE_ENTITY,
			SERVER_CLIENT_DISCONNECT,
			SERVER_PLAYER_ID,
			CONSOLE_MESSAGE,
			CLIENT_BREAK_BLOCK, //also SERVER_ADD_ENTITY
			SERVER_REMOVE_BLOCK
	);
	
	public static boolean isReliable(short protocol) {
		return RELIABLE_PROTOCOLS.contains(protocol);
	}
	
	public static byte[] buildConsoleMessage(String message) {
		byte[] msg = message.getBytes(Protocol.CHARSET);
		byte[] packet = new byte[2 + msg.length];
		ByteUtil.putShort(packet, 0, Protocol.CONSOLE_MESSAGE);
		ByteUtil.copy(msg, packet, 2);
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