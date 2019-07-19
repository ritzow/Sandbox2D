package ritzow.sandbox.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * Contains various utility methods for reading and writing primitive and non-primitive types from and to byte arrays.
 * @author Solomon Ritzow
 */
public final class Bytes {
	public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	
	public static void checkCapacity(byte[] dest, int offset, int count) {
		if(count > dest.length - offset)
			throw new IllegalStateException("capacity exceeded");
	}
	
	/**
	 * Splits source into arrays of at most {@code length} bytes.
	 * @param source the source array.
	 * @param length the maximum number of bytes in each returned array.
	 * @param padding number of bytes of extra space to put at the beginning of each destination array.
	 * @param extraArrays number of extra null slots to add at the beginning of the returned arrays.
	 * @return an array of the split arrays.
	 */
	public static byte[][] split(byte[] source, int length, int padding, int extraArrays) {
		int arraycount = (source.length/length) + (source.length % length > 0 ? 1 : 0);
		byte[][] dest = new byte[extraArrays + arraycount][];
		for(int i = extraArrays, pos = 0; i <= arraycount; i++) {
			int remaining = source.length - pos;
			int dataSize = Math.min(length, remaining);
			byte[] packet = new byte[padding + dataSize];
			System.arraycopy(source, pos, packet, padding, dataSize);
			dest[i] = packet;
			pos += dataSize;
		}
		return dest;
	}
	
	public static <T extends Transportable> byte[] serializeCollection(Collection<T> elements, Serializer ser) {
		byte[][] serialized = new byte[elements.size()][]; //store each serialized object's data
		int bytes = 0; //for counting total number of bytes
		int index = 0; //for putting each serialized object into the correct array index
		for(T element : elements) {
			byte[] s = ser.serialize(element);
			serialized[index] = s;
			bytes += s.length;
			index++;
		}
		
		byte[] finalized = concatenate(4, bytes, serialized);
		putInteger(finalized, 0, index);
		return finalized;
	}
	
	public static byte[] serializeArray(Transportable[] elements, Serializer ser) {
		return serializeCollection(Arrays.asList(elements), ser);
	}
	
	/**
	 * Places a set of byte arrays into a destination array consecutively starting at the given offset.
	 * WARNING: This method has very few safety checks
	 * @param destination the array to copy data into
	 * @param offset the starting index into the destination array
	 * @param headerSize number of bytes at the beginning of each array to ignore
	 * @param arrays the arrays to copy
	 * @return the number of bytes written to the destination array
	 */
	public static int concatenate(byte[] destination, int offset, int headerSize, byte[]... arrays) {
		if(offset < 0)
			throw new IndexOutOfBoundsException("offset cannot be less than zero");
		int index = offset;
		for(byte[] array : arrays) {
			System.arraycopy(array, headerSize, destination, index, array.length - headerSize);
			//copy(array, destination, index);
			index += array.length;
		}
		return index - offset;
	}
	
	public static int concatenate(byte[] destination, int offset, byte[]... arrays) {
		return concatenate(destination, offset, 0, arrays);
	}
	
	/**
	 * Creates a byte array containing the supplied arrays starting at a given offset into the output array
	 * @param offset the starting index of the array data
	 * @param arrays the arrays to concatenate
	 * @return a new byte array containing the provided arrays
	 */
	public static byte[] concatenate(int offset, byte[]... arrays) {
		int length = offset;
		for(byte[] array : arrays) 
			length += array.length;
		return concatenate(offset, length, arrays);
	}
	
	private static byte[] concatenate(int offset, int totalBytes, byte[]... arrays) {
		byte[] dest = new byte[offset + totalBytes];
		concatenate(dest, offset, arrays);
		return dest;
	}
	
	public static byte[] concatenate(byte[]... arrays) {
		return concatenate(0, arrays);
	}
	
	/**
	 * @param source the array to copy
	 * @param destination the destination array
	 * @param offset the offset into the destination array
	 */
	public static void copy(byte[] source, byte[] destination, int offset) {
		System.arraycopy(source, 0, destination, offset, source.length);
	}
	
	public static byte[] subArray(byte[] array, int offset, int length) {
		byte[] sub = new byte[length];
		System.arraycopy(array, offset, sub, 0, length);
		return sub;
	}
	
	/* bytes to values */
	
	public static double getDouble(byte[] array, int index) {
		return Double.longBitsToDouble(Bytes.getLong(array, index));
	}
	
	public static float getFloat(byte[] array, int index) {
		return Float.intBitsToFloat(getInteger(array, index));
	}
	
	public static int getInteger(byte[] array, int index) {
		return ((array[index] & 255) << 24) | ((array[index + 1] & 255) << 16) | ((array[index + 2] & 255) << 8) | ((array[index + 3] & 255) << 0);
	}
	
	public static long getLong(byte[] array, int index) {
		long value = 0;
		for (int i = 0; i < 8; i++) {
			value = (value << 8) + (array[index + i] & 0xff);
		}
		return value;
	}
	
	public static short getShort(byte[] array, int index) {
		return (short)((array[index] << 8) | (array[index + 1] & 255));
	}
	
	public static boolean getBoolean(byte[] array, int index) {
		return array[index] == 1 ? true : false;
	}
	
	public static String getString(byte[] array, int index, Charset charset) {
		return new String(array, index + 4, getInteger(array, index), charset);
	}
	
	public static int getStringLength(byte[] array, int index) {
		return getInteger(array, index) + 4;
	}
	
	/* values to bytes */
	
	public static void putDouble(byte[] array, int index, double value) {
		putLong(array, index, Double.doubleToRawLongBits(value));
	}
	
	public static void putFloat(byte[] array, int index, float value) {
		putInteger(array, index, Float.floatToRawIntBits(value));
	}
	
	public static void putInteger(byte[] array, int index, int value) {
		array[index + 0] = (byte)(value >>> 24);
		array[index + 1] = (byte)(value >>> 16);
		array[index + 2] = (byte)(value >>> 8);
		array[index + 3] = (byte)(value >>> 0);
	}
	
	public static void putLong(byte[] array, int index, long value) {
	    for (int i = 7; i >= 0; i--) {
	        array[index + i] = (byte)(value & 255);
	        value >>= 8;
	    }
	}
	
	public static void putShort(byte[] array, int index, short value) {
		array[index + 0] = (byte)((value >>> 8) & 0xFF);
		array[index + 1] = (byte)((value >>> 0) & 0xFF);
	}
	
	public static void putBoolean(byte[] array, int index, boolean b) {
		array[index] = (byte)(b ? 1 : 0);
	}
	
	public static int putBytesWithLength(byte[] array, int index, byte[] bytes) {
		putInteger(array, index, bytes.length);
		copy(bytes, array, index + 4);
		return bytes.length + 4;
	}
	
	public static int putString(byte[] array, int index, String str, Charset charset) {
		return putBytesWithLength(array, index, str.getBytes(charset));
	}
	
	//TODO add custom optimized huffman-coding compression, especially regarding World/Entity data.
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
			DeflaterOutputStream deflater = new DeflaterOutputStream(new DeflaterOutputStream(out));
			deflater.write(data);
			deflater.close();
			return out.toByteArray();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] decompress(byte[] data, int offset, int length) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(length);
			InflaterOutputStream inflater = new InflaterOutputStream(new InflaterOutputStream(out));
			inflater.write(data, offset, length);
			inflater.close();
			return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] decompress(byte[] data) {
		return decompress(data, 0, data.length);
	}
	
	public static byte[] of(short value) {
		byte[] packet = new byte[2];
		Bytes.putShort(packet, 0, value);
		return packet;
	}
	
	public static byte[] of(int value) {
		byte[] packet = new byte[4];
		Bytes.putInteger(packet, 0, value);
		return packet;
	}
	
	public static byte[] of(long value) {
		byte[] packet = new byte[8];
		Bytes.putLong(packet, 0, value);
		return packet;
	}	
	
	public static byte[] of(float value) {
		byte[] packet = new byte[4];
		Bytes.putFloat(packet, 0, value);
		return packet;
	}
	
	public static byte[] of(double value) {
		byte[] packet = new byte[8];
		Bytes.putDouble(packet, 0, value);
		return packet;
	}
}
