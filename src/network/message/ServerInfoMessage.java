package network.message;

import networkutils.Message;
import static networkutils.ByteUtil.*;

import networkutils.InvalidMessageException;

public class ServerInfoMessage extends Message {
	
	protected short playerCount;
	protected short playerCapacity;
	
	public ServerInfoMessage(byte[] packet) throws InvalidMessageException {
		if(packet.length < 6)
			throw new InvalidMessageException();
		this.playerCount = getShort(packet, 2);
		this.playerCapacity = getShort(packet, 4);
		
	}
	
	public ServerInfoMessage(short playerCount, short playerCapacity) {
		this.playerCount = playerCount;
		this.playerCapacity = playerCapacity;
	}

	@Override
	public byte[] getBytes() {
		byte[] message = new byte[6];
		putShort(message, 0, (short)3);
		putShort(message, 2, playerCount);
		putShort(message, 4, playerCapacity);
		return message;
	}

	public final short getPlayerCount() {
		return playerCount;
	}

	public final short getPlayerCapacity() {
		return playerCapacity;
	}

	@Override
	public String toString() {
		return "Player Count: " + playerCount + "/" + playerCapacity;
	}

}
