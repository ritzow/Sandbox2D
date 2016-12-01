package network.message;

import java.net.SocketAddress;

public interface MessageHandler {
	default void handle(ClientInfo message, SocketAddress sender) {}
	default void handle(ServerConnectAcknowledgment message, SocketAddress sender) {}
	default void handle(ServerConnectRequest message, SocketAddress sender) {}
	default void handle(ServerInfo message, SocketAddress sender) {}
	default void handle(ServerInfoRequest message, SocketAddress sender) {}
	default void handle(WorldCreationMessage message, SocketAddress sender) {}
	default void handle(BlockGridChunkMessage message, SocketAddress sender) {}
}
