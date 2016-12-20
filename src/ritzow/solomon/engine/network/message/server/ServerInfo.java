package ritzow.solomon.engine.network.message.server;

import static ritzow.solomon.engine.util.ByteUtil.getInteger;
import static ritzow.solomon.engine.util.ByteUtil.putInteger;

import ritzow.solomon.engine.network.message.InvalidMessageException;
import ritzow.solomon.engine.network.message.Message;

public class ServerInfo implements Message {
	
	protected int playerCount;
	protected int playerCapacity;
	
	public ServerInfo(byte[] data) throws InvalidMessageException {
		this.playerCount = getInteger(data, 0);
		this.playerCapacity = getInteger(data, 4);
	}
	
	public ServerInfo(int playerCount, int playerCapacity) {
		this.playerCount = playerCount;
		this.playerCapacity = playerCapacity;
	}

	@Override
	public byte[] getBytes() {
		byte[] message = new byte[8];
		putInteger(message, 0, playerCount);
		putInteger(message, 4, playerCapacity);
		return message;
	}

	public final int getPlayerCount() {
		return playerCount;
	}

	public final int getPlayerCapacity() {
		return playerCapacity;
	}

	@Override
	public String toString() {
		return "Player Count: " + playerCount + "/" + playerCapacity;
	}

	@Override
	public boolean isReliable() {
		return true;
	}

}
