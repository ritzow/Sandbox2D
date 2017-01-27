package ritzow.solomon.engine.network;

import java.net.SocketAddress;
import ritzow.solomon.engine.world.entity.Player;

public class ClientState {
	protected int lastMessageID;
	protected Player player;
	protected String username;
	protected SocketAddress address;
	
	public ClientState(SocketAddress address) {
		this.address = address;
		lastMessageID = 0;
		player = null;
		username = "anonymous";
	}

	public final int getLastMessageID() {
		return lastMessageID;
	}

	public final Player getPlayer() {
		return player;
	}

	public final String getUsername() {
		return username;
	}

	public final SocketAddress getAddress() {
		return address;
	}

	public final void setLastMessageID(int lastMessageID) {
		this.lastMessageID = lastMessageID;
	}

	public final void setPlayer(Player player) {
		this.player = player;
	}

	public final void setUsername(String username) {
		this.username = username;
	}
}
