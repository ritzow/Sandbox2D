package util;

import java.io.*;

public final class ByteUtil {
	
	//Data composition from bytes
	
	public static short getShort(byte a, byte b) {
		return (short)((a << 8) + (b << 0));
	}
	
	public static short getShort(byte[] array, int index) {
		return (short)((array[index] << 8) | (array[index + 1] & 0xff));
	}
	
	public static int getInteger(byte[] array, int index) {
		return ((array[index] & 255) << 24) | ((array[index + 1] & 255) << 16) | ((array[index + 2] & 255) << 8) | ((array[index + 3] & 255) << 0);
	}
	
	public static float getFloat(byte a, byte b, byte c, byte d) {
		return Float.intBitsToFloat(((a & 255) << 24) | ((b & 255) << 16) | ((c & 255) << 8) | ((d & 255) << 0));
	}
	
	public static float getFloat(byte[] array, int index) {
		return getFloat(array[index], array[index + 1], array[index + 2], array[index + 3]);
	}
	
	public static boolean getBoolean(byte[] array, int index) {
		return array[index] == 1 ? true : false;
	}
	
	public static synchronized Object deserialize(byte[] data, int offset, int length) throws ClassNotFoundException, IOException {
		return new ObjectInputStream(new ByteArrayInputStream(data, offset, length)).readObject();
	}
	
	//Data decomposition to bytes
	
	public static void putFloat(byte[] array, int index, float value) {
		int temp = Float.floatToRawIntBits(value);
		array[index + 0] = (byte)((temp >>> 24) & 0xFF);
		array[index + 1] = (byte)((temp >>> 16) & 0xFF);
		array[index + 2] = (byte)((temp >>>  8) & 0xFF);
		array[index + 3] = (byte)((temp >>>  0) & 0xFF);
	}
	
	public static void putShort(byte[] array, int index, short value) {
		array[index + 0] = (byte)(((int)value >>> 8) & 0xFF);
		array[index + 1] = (byte)(((int)value >>> 0) & 0xFF);
	}
	
	public static void putInteger(byte[] array, int index, int value) {
		array[index + 0] = (byte)(value >>> 24);
		array[index + 1] = (byte)(value >>> 16);
		array[index + 2] = (byte)(value >>> 8);
		array[index + 3] = (byte)(value >>> 0);
	}
	
	public static void putBoolean(byte[] array, int index, boolean b) {
		array[index] = (byte) (b ? 1 : 0);
	}
	
	public static synchronized byte[] serialize(Serializable object) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream ser = new ObjectOutputStream(out);
		ser.writeObject(object);
		return out.toByteArray();
	}
}
