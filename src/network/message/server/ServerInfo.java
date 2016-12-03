package network.message.server;

import static util.ByteUtil.*;

import java.net.DatagramPacket;
import network.message.InvalidMessageException;
import network.message.Message;
import network.message.Protocol;

public class ServerInfo extends Message {
	
	protected int playerCount;
	protected int playerCapacity;
	
	public ServerInfo(DatagramPacket packet) throws InvalidMessageException {
		if(packet.getLength() < 10 || getShort(packet.getData(), packet.getOffset()) != Protocol.SERVER_INFO)
			throw new InvalidMessageException();
		this.playerCount = getInteger(packet.getData(), packet.getOffset() + 2);
		this.playerCapacity = getInteger(packet.getData(), packet.getOffset() + 6);
	}
	
	public ServerInfo(int playerCount, int playerCapacity) {
		this.playerCount = playerCount;
		this.playerCapacity = playerCapacity;
	}

	@Override
	public byte[] getBytes() {
		byte[] message = new byte[10];
		putShort(message, 0, Protocol.SERVER_INFO);
		putInteger(message, 2, playerCount);
		putInteger(message, 6, playerCapacity);
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

}
