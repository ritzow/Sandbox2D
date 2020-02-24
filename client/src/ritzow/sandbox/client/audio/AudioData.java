package ritzow.sandbox.client.audio;

import java.nio.ByteBuffer;

public interface AudioData {
	public short getBitsPerSample();
	public boolean isSigned();
	public short getChannels();
	public int getSampleRate();
	public ByteBuffer getData();
}