package network;

import network.message.*;

public interface MessageHandler {
	void handleMessage(ClientInfoMessage message);
	void handleMessage(EntityUpdateMessage message);
	void handleMessage(ServerConnectAcknowledgement message);
	void handleMessage(ServerConnectRequest messsage);
	void handleMessage(ServerInfoMessage message);
	void handleMessage(ServerInfoRequest message);
}
