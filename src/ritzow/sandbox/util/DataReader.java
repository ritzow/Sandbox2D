package ritzow.sandbox.util;

public interface DataReader {
	public double readDouble();
	public float readFloat();
	public long readLong();
	public int readInteger();
	public short readShort();
	public boolean readBoolean();
	public boolean[] readCompactBooleans();
	public byte readByte();
	public byte[] readBytes(int count);
	public void readBytes(byte[] dest, int offset);
	public <T> T readObject();
}
