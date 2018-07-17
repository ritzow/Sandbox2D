package ritzow.sandbox.data;

public interface DataReader {
	int remaining();
	void skip(int bytes);
	double readDouble();
	float readFloat();
	long readLong();
	int readInteger();
	short readShort();
	boolean readBoolean();
	boolean[] readCompactBooleans();
	byte readByte();
	byte[] readBytes(int count);
	void readBytes(byte[] dest, int offset);
}
