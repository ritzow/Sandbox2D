package ritzow.sandbox.data;

public interface DataWriter {
	public void writeDouble(double value);
	public void writeFloat(float value);
	public void writeLong(long value);
	public void writeInteger(int value);
	public void writeShort(short value);
	public void writeBoolean(boolean value);
	public void writeByte(byte value);
	public void writeBytes(byte[] data);
	public void writeBytes(byte[] data, int offset, int length);
}
