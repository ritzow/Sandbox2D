package ritzow.sandbox.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * Contains various utility methods for reading and writing primitive and non-primitive types from and to byte arrays.
 * @author Solomon Ritzow
 *
 */
public final class ByteUtil {
	
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
	 * @param arrays the arrays to copy
	 * @return the number of bytes written to the destination array
	 */
	public static int concatenate(byte[] destination, int offset, byte[]... arrays) {
		if(offset < 0)
			throw new IndexOutOfBoundsException("offset cannot be less than zero");
		int index = offset;
		for(byte[] array : arrays) {
			copy(array, destination, index);
			index += array.length;
		}
		return index - offset;
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
	 * @param original the array to copy
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
	
	/* Data composition from bytes */
	
	public static float getFloat(byte[] array, int index) {
		return Float.intBitsToFloat(getInteger(array, index));
	}
	
	public static int getInteger(byte[] array, int index) {
		return ((array[index] & 255) << 24) | ((array[index + 1] & 255) << 16) | ((array[index + 2] & 255) << 8) | ((array[index + 3] & 255) << 0);
	}
	
	public static short getShort(byte[] array, int index) {
		return (short)((array[index] << 8) | (array[index + 1] & 255));
	}
	
	public static boolean getBoolean(byte[] array, int index) {
		return array[index] == 1 ? true : false;
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
	
//	public static Object deserialize(byte[] object) throws ReflectiveOperationException {
//		return deserialize(object, 0);
//	}
	
//	public static Object deserialize(byte[] array, int offset) throws ReflectiveOperationException {
//		return deserialize("", array, offset);
//	}
	
//	public static Object deserialize(String assumedPackage, byte[] array, int offset) throws ReflectiveOperationException {
//		int nameLength = getInteger(array, offset);
//		if(nameLength == 0)
//			return null;
//		int objectPos = offset + 4 + nameLength + 4;
//		return Class.forName(assumedPackage + new String(array, offset + 4, nameLength)).getConstructor(byte[].class)
//				.newInstance(Arrays.copyOfRange(array, objectPos, objectPos + getInteger(array, objectPos - 4)));
//	}
	
//	/** Deserializes {@code count} entities from the array starting at position {@code offset} 
//	 * @throws ReflectiveOperationException **/
//	public static Transportable[] deserialize(byte[] array, int offset, int count) throws ReflectiveOperationException {
//		Transportable[] objects = new Transportable[count];
//		int index = offset;
//		int num = 0;
//		
//		while(num < count) {
//			deserialize(array, index);
//			index += getSerializedLength(array, index);
//			num++;
//		}
//		
//		return objects;
//	}
	
//	public static int getSerializedLength(byte[] array, int offset) {
//		int classLength = getInteger(array, offset);
//		if(classLength == 0)
//			return 4;
//		int objectLength = getInteger(array, offset + 4 + classLength);
//		return 4 + classLength + 4 + objectLength;
//	}
	
//	public static byte[] serialize(Transportable object) {
//		return serialize("", object);
//	}
	
//	/**
//	 * Serialized an object into a byte array. Format: [4 bytes : length of class name string] + [class name] + [4 bytes : object data length] + [object data]
//	 * @param object the Transportable object to serialize
//	 * @return a byte array representing a serialized version of {@code object}
//	 */
//	public static byte[] serialize(String assumedPackage, Transportable object) {
//		if(object == null)
//			return new byte[4]; //return a name length of 0, which is interpreted by deserialize as null
//		
//		String className = object.getClass().getName();
//		int packageIndex = className.indexOf(assumedPackage);
//		
//		if(packageIndex == -1) {
//			throw new UnsupportedOperationException("Invalid parent package");
//		}
//		
//		byte[] nameBytes = className.substring(packageIndex + assumedPackage.length()).getBytes(CHARSET);
//		byte[] objectBytes = object.getBytes(null);
//
//		//class name length, class name, object data length, object data
//		byte[] data = new byte[4 + nameBytes.length + 4 + objectBytes.length];
//		
//		//first four bytes are the length of the class name
//		putInteger(data, 0, nameBytes.length);
//		
//		//put the class name in the following bytes
//		copy(nameBytes, data, 4);
//		
//		//put the length of the object data in the next four bytes after class name
//		putInteger(data, 4 + nameBytes.length, objectBytes.length);
//		
//		//put the object data into the final byte array
//		copy(objectBytes, data, 4 + nameBytes.length + 4);
//		
//		return data;
//	}
}
