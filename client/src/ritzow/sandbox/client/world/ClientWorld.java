package ritzow.sandbox.client.world;

import ritzow.sandbox.audio.AudioSystem;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.world.World;

public final class ClientWorld extends World {

	public ClientWorld(AudioSystem audio, int width, int height, float gravity) {
		super(audio, width, height, gravity);
	}
	
	public ClientWorld(DataReader reader) {
		super(reader);
	}
}