package ritzow.solomon.engine.network.message;

import java.net.SocketAddress;
import ritzow.solomon.engine.network.message.client.ClientInfo;
import ritzow.solomon.engine.network.message.client.ServerConnectRequest;
import ritzow.solomon.engine.network.message.client.ServerInfoRequest;
import ritzow.solomon.engine.network.message.client.WorldRequest;
import ritzow.solomon.engine.network.message.server.ServerConnectAcknowledgment;
import ritzow.solomon.engine.network.message.server.ServerInfo;
import ritzow.solomon.engine.network.message.server.world.WorldCreationMessage;
import ritzow.solomon.engine.network.message.server.world.WorldDataMessage;

/**
 * Provides a way to process different message types on an opt-in basis
 * @author Solomon Ritzow
 */
public interface MessageHandler {
	default void handle(ClientInfo message, SocketAddress sender, int messageID) {}
	default void handle(ServerConnectAcknowledgment message, SocketAddress sender, int messageID) {}
	default void handle(ServerConnectRequest message, SocketAddress sender, int messageID) {}
	default void handle(ServerInfo message, SocketAddress sender, int messageID) {}
	default void handle(ServerInfoRequest message, SocketAddress sender, int messageID) {}
	default void handle(WorldCreationMessage message, SocketAddress sender, int messageID) {}
	default void handle(WorldDataMessage message, SocketAddress sender, int messageID) {}
	default void handle(WorldRequest message, SocketAddress sender, int messageID) {}
}
