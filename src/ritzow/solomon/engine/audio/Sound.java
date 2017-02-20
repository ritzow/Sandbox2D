package ritzow.solomon.engine.audio;

import ritzow.solomon.engine.util.Transportable;

public class Sound implements Transportable {
	public final int sound;
	public volatile float posX;
	public volatile float posY;
	public volatile float velocityX;
	public volatile float velocityY;
	public volatile float gain;
	public volatile float pitch;
	
	public Sound(int sound, float posX, float posY, float velocityX, float velocityY, float gain, float pitch) {
		super();
		this.sound = sound;
		this.posX = posX;
		this.posY = posY;
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.gain = gain;
		this.pitch = pitch;
	}
	
	public Sound(byte[] serialized) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public byte[] getBytes() {
		throw new UnsupportedOperationException("not implemented");
	}
}
