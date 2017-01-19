package ritzow.solomon.engine.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * Contains various utility methods for reading and writing primitive and non-primitive types from and to byte arrays.
 * @author Solomon Ritzow
 *
 */
public final class ByteUtil {
	
	public static byte[] concatenate(byte[]... arrays) {
		int length = 0;
		for(int i = 0; i < arrays.length; i++) {
			length += arrays[i].length;
		}
		
		byte[] concatenated = new byte[length];
		int index = 0;
		for(int i = 0; i < arrays.length; i++) {
			System.arraycopy(arrays[i], 0, concatenated, index, arrays[i].length);
			index += arrays[i].length;
		}
		
		return concatenated;
	}
	
	/**
	 * @param original the array to copy
	 * @param destination the destination array
	 * @param offset the offset into the destination array
	 */
	public static void copy(byte[] original, byte[] destination, int offset) {
		System.arraycopy(original, 0, destination, offset, original.length);
	}
	
	public static byte[] subArray(byte[] array, int offset, int length) {
		byte[] sub = new byte[length];
		System.arraycopy(array, offset, sub, 0, length);
		return sub;
	}
	
	/* Data composition from bytes */
	
	public static float getFloat(byte[] array, int index) {
		return Float.intBitsToFloat(((array[index] & 255) << 24) | ((array[index + 1] & 255) << 16) | ((array[index + 2] & 255) << 8) | ((array[index + 3] & 255) << 0));
	}
	
	public static int getInteger(byte[] array, int index) {
		return ((array[index] & 255) << 24) | ((array[index + 1] & 255) << 16) | ((array[index + 2] & 255) << 8) | ((array[index + 3] & 255) << 0);
	}
	
	public static short getShort(byte[] array, int index) {
		return (short)((array[index] << 8) | (array[index + 1] & 0xff));
	}
	
	public static boolean getBoolean(byte[] array, int index) {
		return array[index] == 1 ? true : false;
	}
	
	public static byte[] decompress(byte[] data) {
		return decompress(data, 0, data.length);
	}
	
	public static byte[] decompress(byte[] data, int offset, int length) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(length);
			InflaterOutputStream inflater = new InflaterOutputStream(out);
			inflater.write(data, offset, length);
			inflater.close();
			byte[] bytes = out.toByteArray();
			out.reset();
			InflaterOutputStream inflater2 = new InflaterOutputStream(out); //decompress a second time
			inflater2.write(bytes);
			inflater2.close();
			return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Object deserialize(byte[] object) throws ReflectiveOperationException {
		return deserialize(object, 0);
	}
	
	public static Object deserialize(byte[] array, int offset) throws ReflectiveOperationException {
		int nameLength = getInteger(array, offset);
		if(nameLength == 0)
			return null;
		int objectPos = offset + 4 + nameLength + 4;
		return deserialize(Class.forName(new String(array, offset + 4, nameLength)), Arrays.copyOfRange(array, objectPos, objectPos + getInteger(array, objectPos - 4)));
	}
	
	public static Object deserialize(Class<?> type, byte[] data) throws ReflectiveOperationException {
		return type.getConstructor(byte[].class).newInstance(data);
	}
	
	public static int getSerializedLength(byte[] array, int offset) {
		int classLength = getInteger(array, offset);
		if(classLength == 0)
			return 4;
		int objectLength = getInteger(array, offset + 4 + classLength);
		return 4 + classLength + 4 + objectLength;
	}
	
	/* Data decomposition to bytes */
	
	public static void putFloat(byte[] array, int index, float value) {
		putInteger(array, index, Float.floatToRawIntBits(value));
	}
	
	public static void putInteger(byte[] array, int index, int value) {
		array[index + 0] = (byte)(value >>> 24);
		array[index + 1] = (byte)(value >>> 16);
		array[index + 2] = (byte)(value >>> 8);
		array[index + 3] = (byte)(value >>> 0);
	}
	
	public static void putShort(byte[] array, int index, short value) {
		array[index + 0] = (byte)((value >>> 8) & 0xFF);
		array[index + 1] = (byte)((value >>> 0) & 0xFF);
	}
	
	public static void putBoolean(byte[] array, int index, boolean b) {
		array[index] = (byte)(b ? 1 : 0);
	}
	
	/**
	 * Compressed byte data using the deflate algorithm provided by java
	 * @param data the byte array to compress
	 * @param times the number of times to compress the data, two times is ideal
	 * @return a new compressed byte array
	 * @throws IOException
	 */
	public static byte[] compress(byte[] data) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DeflaterOutputStream deflater = new DeflaterOutputStream(out);
			deflater.write(data);
			deflater.close();
			byte[] bytes = out.toByteArray();
			out.reset();
			DeflaterOutputStream deflater2 = new DeflaterOutputStream(out); //compress a second time
			deflater2.write(bytes);
			deflater2.close();
			return out.toByteArray();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Serialized an object into a byte array.
	 * Format: [4 bytes : length of class name string] + [class name] + [4 bytes : object data length] + [object data]
	 * @param object
	 * @return
	 */
	public static byte[] serialize(Transportable object) {
		if(object == null)
			return new byte[4]; //return a name length of 0
		
		byte[] nameBytes = object.getClass().getName().getBytes();
		byte[] objectBytes = object.getBytes();
		
		//class name length, class name, object data length, object data
		byte[] data = new byte[4 + nameBytes.length + 4 + objectBytes.length];
		
		//first four bytes are the length of the class name
		putInteger(data, 0, nameBytes.length);
		
		//put the class name in the following bytes
		ByteUtil.copy(nameBytes, data, 4);
		
		//put the length of the object data in the next four bytes after class name
		putInteger(data, 4 + nameBytes.length, objectBytes.length);
		
		//put the object data into the final byte array
		ByteUtil.copy(objectBytes, data, 4 + nameBytes.length + 4);
		
		return data;
	}
}
