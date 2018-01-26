package ritzow.sandbox.client.audio;

import java.nio.ByteBuffer;

public interface SoundInfo {
	public short getBitsPerSample();
	public short getChannels();
	public int getSampleRate();
	public ByteBuffer getData();
}
