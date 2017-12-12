package ritzow.sandbox.data;

public interface DataReader {
	public int remaining();
	public void skip(int bytes);
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
	public <T extends Transportable> T readObject();
}
