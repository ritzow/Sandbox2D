package ritzow.sandbox.util;

public interface DataWriter {
	public void writeDouble(double d);
	public void writeFloat(float f);
	public void writeLong(long l);
	public void writeInteger(int i);
	public void writeShort(short s);
	public void writeByte(byte b);
	public void writeBytes(byte[] data);
	public void writeObject(Transportable object);
}
