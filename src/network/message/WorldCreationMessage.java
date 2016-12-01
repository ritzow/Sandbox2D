package network.message;

import java.net.DatagramPacket;
import static util.ByteUtil.*;
import world.World;

/**
 * @author Solomon Ritzow
 *
 */
public class WorldCreationMessage extends Message {
	
	protected int width;
	protected int height;
	
	public WorldCreationMessage(World world) {
		this.width = world.getForeground().getWidth();
		this.height = world.getForeground().getHeight();
	}
	
	public WorldCreationMessage(DatagramPacket packet) throws InvalidMessageException {
		if(getShort(packet.getData(), packet.getOffset()) != Protocol.WORLD_CREATION_MESSAGE)
			throw new InvalidMessageException();
		this.width = getInteger(packet.getData(), packet.getOffset() + 2);
		this.height = getInteger(packet.getData(), packet.getOffset() + 6);
	}

	@Override
	public byte[] getBytes() {
		byte[] data = new byte[10];
		putShort(data, 0, Protocol.WORLD_CREATION_MESSAGE);
		putInteger(data, 2, width);
		putInteger(data, 6, height);
		return data;
	}

	@Override
	public String toString() {
		return "width: " + width + ", height: " + height;
	}
	
}
