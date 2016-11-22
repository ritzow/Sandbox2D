package network.message;

import static util.ByteUtil.*;

import java.net.DatagramPacket;

public class ServerInfoMessage extends Message {
	
	protected short playerCount;
	protected short playerCapacity;
	
	public ServerInfoMessage(DatagramPacket packet) throws InvalidMessageException {
		if(packet.getLength() < 6 || getShort(packet.getData(), packet.getOffset()) != Protocol.SERVER_INFO)
			throw new InvalidMessageException();
		this.playerCount = getShort(packet.getData(), packet.getOffset() + 2);
		this.playerCapacity = getShort(packet.getData(), packet.getOffset() + 4);
	}
	
	public ServerInfoMessage(short playerCount, short playerCapacity) {
		this.playerCount = playerCount;
		this.playerCapacity = playerCapacity;
	}

	@Override
	public byte[] getBytes() {
		byte[] message = new byte[6];
		putShort(message, 0, Protocol.SERVER_INFO);
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
