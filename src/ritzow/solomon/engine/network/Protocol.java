package ritzow.solomon.engine.network;

import java.util.Arrays;

final class Protocol {
	
	/** The maximum length a sent message can be in bytes **/
	public static final short MAX_MESSAGE_LENGTH = 1000;
	
	/** Client/server message protocol ID **/
	protected static final short
		CLIENT_CONNECT_REQUEST = 2,
		SERVER_CONNECT_ACKNOWLEDGMENT = 3,
		CLIENT_INFO = 4,
		SERVER_WORLD_HEAD = 5,
		SERVER_WORLD_DATA = 6,
		CLIENT_DISCONNECT = 7,
		CLIENT_PLAYER_ACTION = 8,
		SERVER_PLAYER_ENTITY = 9;
	
	public static final short[] RELIABLE_PROTOCOLS = {
		CLIENT_CONNECT_REQUEST,
		SERVER_CONNECT_ACKNOWLEDGMENT,
		CLIENT_INFO,
		SERVER_WORLD_HEAD,
		SERVER_WORLD_DATA,
		CLIENT_DISCONNECT,
		CLIENT_PLAYER_ACTION,
		SERVER_PLAYER_ENTITY
	};
	
	static {
		Arrays.sort(RELIABLE_PROTOCOLS);
	}
}