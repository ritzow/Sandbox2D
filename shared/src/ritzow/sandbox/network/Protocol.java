package ritzow.sandbox.network;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class Protocol {
	private Protocol() {throw new UnsupportedOperationException("instantiation of Protocol not allowed");}

	//gameplay
	public static final long MAX_UPDATE_TIMESTEP = Utility.millisToNanos(200);
	public static final long MAX_UPDATE_TIME_ALLOWED = Utility.millisToNanos(10000);

	public static final float BLOCK_INTERACT_RANGE = 1000;
	public static final long BLOCK_INTERACT_COOLDOWN_NANOSECONDS = Utility.millisToNanos(0);
	public static final long THROW_COOLDOWN_NANOSECONDS = Utility.millisToNanos(0);

	/** The Charset for text encoding used by the client and server **/
	public static final Charset CHARSET = StandardCharsets.UTF_8;

	public static final boolean COMPRESS_WORLD_DATA = true;

	public static final int DEFAULT_SERVER_PORT = 50000;
	
	public static final long
		TIMEOUT_DISCONNECT = Utility.millisToNanos(1000),
		RESEND_COUNT = 10,
		RESEND_INTERVAL = TIMEOUT_DISCONNECT/RESEND_COUNT;

	public static final int
		HEADER_SIZE = 5,
		MAX_PACKET_SIZE = 1024,
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
		//TODO separate receiving entity and adding to world 
		//(TYPE_RECEIVE_AND_ADD_ENTITY vs TYPE_RECEIVE_ENTITY and TYPE_ADD_ENTITY?)
		TYPE_SERVER_ADD_ENTITY = 5, 
		//TODO separate removing entity from world and deleting entity
		TYPE_SERVER_REMOVE_ENTITY = 6,
		TYPE_SERVER_CLIENT_DISCONNECT = 7,
		TYPE_SERVER_REMOVE_BLOCK = 9,
		TYPE_CLIENT_CONNECT_REQUEST = 10,
		TYPE_CLIENT_DISCONNECT = 11,
		TYPE_CLIENT_PLAYER_STATE = 12,
		TYPE_CLIENT_BREAK_BLOCK = 13,
		TYPE_PING = 14,
		TYPE_CLIENT_BOMB_THROW = 16,
		TYPE_CLIENT_WORLD_BUILT = 17,
		TYPE_CLIENT_PLACE_BLOCK = 18,
		TYPE_SERVER_PLACE_BLOCK = 19;

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

	public static byte[] buildConsoleMessage(String message) {
		byte[] msg = message.getBytes(Protocol.CHARSET);
		byte[] packet = new byte[2 + msg.length];
		Bytes.putShort(packet, 0, Protocol.TYPE_CONSOLE_MESSAGE);
		Bytes.copy(msg, packet, 2);
		return packet;
	}

	public static class PlayerState {
		//PlayerState format:
		//2 bytes message type
		//1 bit left
		//1 bit right
		//1 bit up
		//1 bit down
		//1 bit primary_action
		//1 bit secondary_action
		//1 bit unused
		//1 bit unused
		//1 byte unsigned item slot TODO not currently unsigned
		public static byte MOVE_LEFT = 			0b00000001;
		public static byte MOVE_RIGHT = 		0b00000010;
		public static byte MOVE_UP = 			0b00000100;
		public static byte MOVE_DOWN = 			0b00001000;
		public static byte PRIMARY_ACTION = 	0b00010000;
		public static byte SECONDARY_ACTION = 	0b00100000;
		
		public static boolean isPrimary(short state) {
			return (state & PRIMARY_ACTION) == PRIMARY_ACTION;
		}
		
		public static boolean isSecondary(short state) {
			return (state & SECONDARY_ACTION) == SECONDARY_ACTION;
		}

		public static short getState(PlayerEntity player, boolean primary, boolean secondary) {
			short state = 0;
			state |= player.isLeft() 	? MOVE_LEFT : 0;
			state |= player.isRight() 	? MOVE_RIGHT : 0;
			state |= player.isUp() 		? MOVE_UP : 0;
			state |= player.isDown() 	? MOVE_DOWN : 0;
			state |= primary			? PRIMARY_ACTION : 0;
			state |= secondary			? SECONDARY_ACTION : 0;
			state |= player.selected() << 8;
			return state;
		}

		public static void updatePlayer(PlayerEntity player, short state) {
			player.setLeft((state & MOVE_LEFT) == MOVE_LEFT);
			player.setRight((state & MOVE_RIGHT) == MOVE_RIGHT);
			player.setUp((state & MOVE_UP) == MOVE_UP);
			player.setDown((state & MOVE_DOWN) == MOVE_DOWN);
			player.setSlot(state >>> 8);
		}
	}
}