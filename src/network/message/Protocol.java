package network.message;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import network.message.client.ClientInfo;
import network.message.client.ServerConnectRequest;
import network.message.client.ServerInfoRequest;
import network.message.server.ServerConnectAcknowledgment;
import network.message.server.ServerInfo;
import network.message.server.world.BlockGridChunkMessage;
import network.message.server.world.WorldCreationMessage;
import util.ByteUtil;

/**
 * Network message ID constants
 * @author Solomon Ritzow
 *
 */
public final class Protocol {
	
	/**
	 * Client/server message protocol ID
	 */
	public static final short
		SERVER_INFO_REQUEST = 0,
		SERVER_INFO = 1,
		SERVER_CONNECT_REQUEST = 2,
		SERVER_CONNECT_ACKNOWLEDGMENT = 3,
		CLIENT_INFO = 4,
		ENTITY_UPDATE = 5,
		MULTI_MESSAGE = 6,
		WORLD_CREATION_MESSAGE = 7,
		WORLD_CHUNK_MESSAGE = 8;
	
	public static DatagramPacket construct(int id, short protocol, byte[] message, SocketAddress receiver) {
		byte[] packet = new byte[message.length + 6];
		ByteUtil.putInteger(packet, 0, id);
		ByteUtil.putShort(packet, 4, protocol);
		System.arraycopy(message, 0, packet, 6, message.length);
		return new DatagramPacket(packet, packet.length, receiver);
	}
	
	public static DatagramPacket construct(int id, Message message, SocketAddress receiver) {
		short protocol;
		if(message instanceof ServerConnectRequest) {
			protocol = SERVER_CONNECT_REQUEST;
		} else if(message instanceof ClientInfo) {
			protocol = CLIENT_INFO;
		} else if(message instanceof ServerInfo) {
			protocol = SERVER_INFO;
		} else if(message instanceof ServerInfoRequest) {
			protocol = SERVER_INFO_REQUEST;
		} else if(message instanceof ServerConnectAcknowledgment) {
			protocol = SERVER_CONNECT_ACKNOWLEDGMENT;
		} else if(message instanceof WorldCreationMessage) {
			protocol = WORLD_CREATION_MESSAGE;
		} else if(message instanceof BlockGridChunkMessage) {
			protocol = WORLD_CHUNK_MESSAGE;
		} else {
			throw new RuntimeException("invalid message");
		}
		return construct(id, protocol, message.getBytes(), receiver);
	}
	
	public static void process(int id, int protocol, byte[] data, SocketAddress sender, MessageHandler handler) throws UnknownMessageException, InvalidMessageException {
		switch(protocol) {
			case Protocol.SERVER_CONNECT_REQUEST:
				handler.handle(new ServerConnectRequest(), sender, id);
				return;
			case Protocol.CLIENT_INFO:
				handler.handle(new ClientInfo(data), sender, id);
				return;
			case Protocol.SERVER_INFO:
				handler.handle(new ServerInfo(data), sender, id);
				return;
			case Protocol.SERVER_INFO_REQUEST:
				handler.handle(new ServerInfoRequest(), sender, id);
				return;
			case Protocol.SERVER_CONNECT_ACKNOWLEDGMENT:
				handler.handle(new ServerConnectAcknowledgment(data), sender, id);
				return;
			case Protocol.WORLD_CREATION_MESSAGE:
				handler.handle(new WorldCreationMessage(data), sender, id);
				return;
			case Protocol.WORLD_CHUNK_MESSAGE:
				handler.handle(new BlockGridChunkMessage(data), sender, id);
				return;
			default:
				throw new UnknownMessageException();
		}
	}
}