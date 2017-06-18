package ritzow.sandbox.protocol;

import java.nio.charset.Charset;
import java.util.Arrays;
import ritzow.sandbox.util.ByteUtil;
import ritzow.sandbox.world.entity.PlayerEntity;

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
		SERVER_ADD_ENTITY_COMPRESSED = 7,
		SERVER_REMOVE_ENTITY = 8,
		SERVER_CLIENT_DISCONNECT = 9,
		SERVER_CREATE_LOBBY = 10,
		SERVER_PLAYER_ID = 11,
		SERVER_REMOVE_BLOCK = 12;
	
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
	
	public static class PlayerAction {
		public static final byte
			PLAYER_LEFT = 0,
			PLAYER_RIGHT = 1,
			PLAYER_UP = 2,
			PLAYER_DOWN = 3;
		
		public static final byte[] buildPlayerAction(byte action, boolean enable) {
			byte[] packet = new byte[4];
			ByteUtil.putShort(packet, 0, CLIENT_PLAYER_ACTION);
			packet[2] = action;
			ByteUtil.putBoolean(packet, 3, enable);
			return packet;
		}
		
		public static final void processPlayerAction(byte[] data, PlayerEntity player) {
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
}