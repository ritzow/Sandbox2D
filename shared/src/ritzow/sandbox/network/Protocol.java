package ritzow.sandbox.network;

import java.nio.charset.Charset;
import java.util.Arrays;
import ritzow.sandbox.data.ByteUtil;

public final class Protocol {
	
	/** The Charset used by the client and server **/
	public static final Charset CHARSET = Charset.forName("UTF-8");
	
	/** The maximum length a sent message can be in bytes **/
	public static final short MAX_MESSAGE_LENGTH = 1000;
	
	/** Server message protocol ID **/
	public static final short
		SERVER_CONNECT_ACKNOWLEDGMENT = 1,
		SERVER_WORLD_HEAD = 2,
		SERVER_WORLD_DATA = 3,
		SERVER_ENTITY_LOCATION = 4,
		SERVER_ENTITY_UPDATE = 5,
		SERVER_ADD_ENTITY = 6,
		SERVER_REMOVE_ENTITY = 8,
		SERVER_CLIENT_DISCONNECT = 9,
		SERVER_CREATE_LOBBY = 10,
		SERVER_PLAYER_ID = 11,
		SERVER_REMOVE_BLOCK = 12,
		SERVER_PING = 13;
	
	/** Client message protocol ID **/
	public static final short
		CLIENT_CONNECT_REQUEST = 1,
		CLIENT_INFO = 2,
		CLIENT_DISCONNECT = 3,
		CLIENT_PLAYER_ACTION = 4,
		CLIENT_PING = 5,
		CLIENT_BREAK_BLOCK = 6;
	
	/** Shared protocol ID **/
	public static final short
		CONSOLE_MESSAGE = 0;
	
	private static final short[] RELIABLE_PROTOCOLS = {
		CLIENT_CONNECT_REQUEST,
		CLIENT_DISCONNECT,
		CLIENT_PLAYER_ACTION,
		CLIENT_INFO,
		CLIENT_PING,
		SERVER_CONNECT_ACKNOWLEDGMENT,
		SERVER_WORLD_HEAD,
		SERVER_WORLD_DATA,
		SERVER_ADD_ENTITY,
		SERVER_REMOVE_ENTITY,
		SERVER_CLIENT_DISCONNECT,
		SERVER_PLAYER_ID,
		CONSOLE_MESSAGE,
		CLIENT_BREAK_BLOCK,
		SERVER_REMOVE_BLOCK
	};
	
	static {
		Arrays.sort(RELIABLE_PROTOCOLS);
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
	
	public static boolean isReliable(short protocol) {
		return Arrays.binarySearch(RELIABLE_PROTOCOLS, protocol) >= 0;
	}
	
	public static byte[] buildEmpty(short protocol) {
		byte[] packet = new byte[2];
		ByteUtil.putShort(packet, 0, protocol);
		return packet;
	}
	
	public static byte[] buildConsoleMessage(String message) {
		byte[] msg = message.getBytes(Charset.forName("UTF-8"));
		byte[] packet = new byte[2 + msg.length];
		ByteUtil.putShort(packet, 0, Protocol.CONSOLE_MESSAGE);
		ByteUtil.copy(msg, packet, 2);
		return packet;
	}
}