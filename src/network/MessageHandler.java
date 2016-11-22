package network;

import java.net.SocketAddress;
import network.message.*;

public interface MessageHandler {
	default void handle(ClientInfoMessage message, SocketAddress sender) {
		
	}
	
	default void handle(EntityUpdateMessage message, SocketAddress sender) {
		
	}
	
	default void handle(ServerConnectAcknowledgment message, SocketAddress sender) {
		
	}
	
	default void handle(ServerConnectRequest messsage, SocketAddress sender) {
		
	}
	
	default void handle(ServerInfoMessage message, SocketAddress sender) {
		
	}
	
	default void handle(ServerInfoRequest message, SocketAddress sender) {
		
	}
}
