package ritzow.solomon.engine.network.message.server.world;

import ritzow.solomon.engine.network.message.InvalidMessageException;
import ritzow.solomon.engine.network.message.Message;
import ritzow.solomon.engine.util.ByteUtil;

/**
 * @author Solomon Ritzow
 *
 */
public class WorldCreationMessage implements Message {
	
	protected int size;
	
	public WorldCreationMessage(int size) {
		this.size = size;
	}
	
	public WorldCreationMessage(byte[] data) throws InvalidMessageException {
		this.size = ByteUtil.getInteger(data, 0);
	}
	
	public int getSize() {
		return size;
	}

	@Override
	public byte[] getBytes() {
		byte[] data = new byte[4];
		ByteUtil.putInteger(data, 0, size);
		return data;
	}

	@Override
	public boolean isReliable() {
		return true;
	}
	
}
