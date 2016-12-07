package network.message;

import java.net.SocketAddress;
import network.message.client.ClientInfo;
import network.message.client.ServerConnectRequest;
import network.message.client.ServerInfoRequest;
import network.message.server.ServerConnectAcknowledgment;
import network.message.server.ServerInfo;
import network.message.server.world.BlockGridChunkMessage;
import network.message.server.world.WorldCreationMessage;

/**
 * Provides a way to process different message types on an opt-in basis
 * @author Solomon Ritzow
 */
public interface MessageHandler {
	default void handle(ClientInfo message, SocketAddress sender) {}
	default void handle(ServerConnectAcknowledgment message, SocketAddress sender) {}
	default void handle(ServerConnectRequest message, SocketAddress sender) {}
	default void handle(ServerInfo message, SocketAddress sender) {}
	default void handle(ServerInfoRequest message, SocketAddress sender) {}
	default void handle(WorldCreationMessage message, SocketAddress sender) {}
	default void handle(BlockGridChunkMessage message, SocketAddress sender) {}
}
