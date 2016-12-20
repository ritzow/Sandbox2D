package ritzow.solomon.engine.network.message.client;

import ritzow.solomon.engine.network.message.Message;

public class WorldRequest implements Message {

	@Override
	public byte[] getBytes() {
		return new byte[0];
	}

	@Override
	public boolean isReliable() {
		return true;
	}

}
