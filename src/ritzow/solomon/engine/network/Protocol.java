package ritzow.solomon.engine.network;

import java.util.Arrays;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.entity.Entity;
import ritzow.solomon.engine.world.entity.PlayerEntity;

public final class Protocol {
	
	/** The maximum length a sent message can be in bytes **/
	public static final short MAX_MESSAGE_LENGTH = 1000;
	
	/** Client/server message protocol ID **/
	public static final short
		CLIENT_CONNECT_REQUEST = 1,
		SERVER_CONNECT_ACKNOWLEDGMENT = 2,
		CLIENT_INFO = 3,
		SERVER_WORLD_HEAD = 4,
		SERVER_WORLD_DATA = 5,
		CLIENT_DISCONNECT = 6,
		CLIENT_PLAYER_ACTION = 7,
		SERVER_PLAYER_ENTITY = 8,
		SERVER_ENTITY_LOCATION = 9,
		CONSOLE_MESSAGE = 10,
		GENERIC_ENTITY_UPDATE = 11,
		SERVER_ADD_ENTITY = 12;
	
	protected static final short[] RELIABLE_PROTOCOLS = {
		CLIENT_CONNECT_REQUEST,
		SERVER_CONNECT_ACKNOWLEDGMENT,
		CLIENT_INFO,
		SERVER_WORLD_HEAD,
		SERVER_WORLD_DATA,
		CLIENT_DISCONNECT,
		CLIENT_PLAYER_ACTION,
		SERVER_PLAYER_ENTITY,
		CONSOLE_MESSAGE,
		SERVER_ADD_ENTITY
	};
	
	static {
		Arrays.sort(RELIABLE_PROTOCOLS);
	}
	
	public static final byte[] buildGenericEntityUpdate(Entity e) {
		//protocol, id, posX, posY, velX, velY
		byte[] update = new byte[2 + 4 + 4 + 4 + 4 + 4];
		ByteUtil.putShort(update, 0, GENERIC_ENTITY_UPDATE);
		ByteUtil.putInteger(update, 2, e.getID());
		ByteUtil.putFloat(update, 6, e.getPositionX());
		ByteUtil.putFloat(update, 10, e.getPositionY());
		ByteUtil.putFloat(update, 14, e.getVelocityX());
		ByteUtil.putFloat(update, 18, e.getVelocityY());
		return update;
	}
	
	public static final byte[] buildEntityTransfer(Entity e) {
		byte[] serialized = ByteUtil.serialize(e);
		byte[] packet = new byte[2 + serialized.length];
		ByteUtil.putShort(packet, 0, SERVER_ADD_ENTITY);
		ByteUtil.copy(serialized, packet, 2);
		return packet;
	}
	
	public static final byte[] buildCompressedEntityTransfer(Entity e) { //TODO needs separate protocol
		byte[] serialized = ByteUtil.compress(ByteUtil.serialize(e));
		byte[] packet = new byte[2 + serialized.length];
		ByteUtil.putShort(packet, 0, SERVER_ADD_ENTITY);
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
}