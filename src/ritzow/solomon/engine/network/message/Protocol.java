package ritzow.solomon.engine.network.message;

import ritzow.solomon.engine.util.ByteUtil;

/**
 * Network message ID constants
 * @author Solomon Ritzow
 *
 */
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
		CLIENT_INFO = 4;
	
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