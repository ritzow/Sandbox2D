package ritzow.sandbox.client.audio;

import java.nio.ByteBuffer;

public interface SoundData {
	public short getBitsPerSample();
	public short getChannels();
	public int getSampleRate();
	public ByteBuffer getData();
}
