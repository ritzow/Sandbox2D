package network.message;

import java.net.DatagramPacket;
import network.message.client.ClientInfo;
import network.message.client.ServerConnectRequest;
import network.message.client.ServerInfoRequest;
import network.message.server.ServerConnectAcknowledgment;
import network.message.server.ServerInfo;
import network.message.server.world.BlockGridChunkMessage;
import network.message.server.world.WorldCreationMessage;

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
	
	public static void processPacket(DatagramPacket packet, MessageHandler handler) throws UnknownMessageException, InvalidMessageException {
		if(packet.getData().length < 2)
			throw new InvalidMessageException();
		try {
			switch(util.ByteUtil.getShort(packet.getData(), 0)) {
			case Protocol.SERVER_CONNECT_REQUEST:
				handler.handle(new ServerConnectRequest(), packet.getSocketAddress());
				return;
			case Protocol.CLIENT_INFO:
				handler.handle(new ClientInfo(packet), packet.getSocketAddress());
				return;
			case Protocol.SERVER_INFO:
				handler.handle(new ServerInfo(packet), packet.getSocketAddress());
				return;
			case Protocol.SERVER_INFO_REQUEST:
				handler.handle(new ServerInfoRequest(), packet.getSocketAddress());
				return;
			case Protocol.SERVER_CONNECT_ACKNOWLEDGMENT:
				handler.handle(new ServerConnectAcknowledgment(packet), packet.getSocketAddress());
				return;
			case Protocol.WORLD_CREATION_MESSAGE:
				handler.handle(new WorldCreationMessage(packet), packet.getSocketAddress());
				return;
			case Protocol.WORLD_CHUNK_MESSAGE:
				handler.handle(new BlockGridChunkMessage(packet), packet.getSocketAddress());
				return;
			default:
				throw new UnknownMessageException();
			}
		} catch(InvalidMessageException e) {
			System.err.println("Caught invalid message");
		}
	}
}