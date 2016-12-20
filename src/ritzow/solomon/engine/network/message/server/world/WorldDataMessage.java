package ritzow.solomon.engine.network.message.server.world;

import ritzow.solomon.engine.network.message.Message;

/**
 * @author Solomon Ritzow
 *
 */
public class WorldDataMessage implements Message {
	
	protected byte[] data;
	
	public WorldDataMessage(byte[] data) {
		this.data = data;
	}

	@Override
	public byte[] getBytes() {
		return data;
	}
	
	public byte[] getData() {
		return getBytes();
	}

	@Override
	public boolean isReliable() {
		return true;
	}

}
