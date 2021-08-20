package ritzow.sandbox.client.audio;

import java.nio.ByteBuffer;

public interface AudioData {
	short getBitsPerSample();
	boolean isSigned();
	short getChannels();
	int getSampleRate();
	ByteBuffer getData();
}
