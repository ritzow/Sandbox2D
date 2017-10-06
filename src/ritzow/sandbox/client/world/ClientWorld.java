package ritzow.sandbox.client.world;

import ritzow.sandbox.audio.AudioSystem;
import ritzow.sandbox.util.DataReader;
import ritzow.sandbox.world.AbstractWorld;

public final class ClientWorld extends AbstractWorld {

	public ClientWorld(AudioSystem audio, int width, int height, float gravity) {
		super(audio, width, height, gravity);
	}
	
	public ClientWorld(DataReader reader) {
		super(reader);
	}
}