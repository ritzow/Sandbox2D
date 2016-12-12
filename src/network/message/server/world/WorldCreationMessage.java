package network.message.server.world;

import network.message.InvalidMessageException;
import network.message.Message;

import static util.ByteUtil.*;
import world.World;

/**
 * @author Solomon Ritzow
 *
 */
public class WorldCreationMessage implements Message {
	
	protected int width;
	protected int height;
	protected float gravity;
	
	public WorldCreationMessage(World world) {
		this.width = world.getForeground().getWidth();
		this.height = world.getForeground().getHeight();
		this.gravity = world.getGravity();
	}
	
	public WorldCreationMessage(byte[] data) throws InvalidMessageException {
		this.width = getInteger(data, 0);
		this.height = getInteger(data, 4);
		this.gravity = getInteger(data, 8);
	}

	@Override
	public byte[] getBytes() {
		byte[] data = new byte[12];
		putInteger(data, 0, width);
		putInteger(data, 4, height);
		putFloat(data, 8, gravity);
		return data;
	}

	@Override
	public String toString() {
		return "width: " + width + ", height: " + height + ", gravity: " + gravity;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public float getGravity() {
		return gravity;
	}
	
}
