package ritzow.solomon.engine.network;

import java.net.SocketAddress;
import ritzow.solomon.engine.world.entity.Player;

class ClientState {
	protected volatile int unreliableMessageID;
	protected volatile int reliableMessageID;
	protected volatile Player player;
	protected volatile String username;
	protected final SocketAddress address;
	
	public ClientState(SocketAddress address) {
		this.address = address;
		player = null;
		username = "unknown " + address.toString();
	}
}
