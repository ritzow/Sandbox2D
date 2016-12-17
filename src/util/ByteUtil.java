package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

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
	
	public static byte[] decompress(byte[] data, int times) throws IOException {
		return decompress(data, 0, data.length, times);
	}
	
	public static byte[] decompress(byte[] data, int offset, int length, int times) throws IOException {
		if(times > 0) {
			ByteArrayOutputStream output = new ByteArrayOutputStream(length);
			InflaterOutputStream inflater = new InflaterOutputStream(output);
			inflater.write(data, offset, length);
			inflater.close();
			byte[] bytes = output.toByteArray();
			return decompress(bytes, 0, bytes.length, times - 1);
		} else {
			return data;
		}
	}
	
	public static <T extends Transportable> T deserialize(byte[] object) throws ReflectiveOperationException {
		return deserialize(object, 0);
	}
	
	public static <T extends Transportable> T deserialize(byte[] array, int offset) throws ReflectiveOperationException {
		int nameLength = ByteUtil.getInteger(array, offset);
		int objectPos = offset + 4 + nameLength + 4;
		String name = new String(array, offset + 4, nameLength);
		if(name.equals("null"))
			return null;
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>)(Class.forName(name));
		return deserialize(clazz, Arrays.copyOfRange(array, objectPos, objectPos + ByteUtil.getInteger(array, objectPos - 4)));
	}
	
	public static <T extends Transportable> T deserialize(Class<T> type, byte[] data) throws ReflectiveOperationException {
		return type.getConstructor(byte[].class).newInstance(data);
	}
	
	public static int getSerializedLength(byte[] array, int offset) {
		int classLength = ByteUtil.getInteger(array, offset);
		int objectLength = ByteUtil.getInteger(array, offset + 4 + classLength);
		return 4 + classLength + 4 + objectLength;
	}
	
	/* Data decomposition to bytes */
	
	public static void putFloat(byte[] array, int index, float value) {
		int temp = Float.floatToRawIntBits(value);
		array[index + 0] = (byte)((temp >>> 24) & 0xFF);
		array[index + 1] = (byte)((temp >>> 16) & 0xFF);
		array[index + 2] = (byte)((temp >>>  8) & 0xFF);
		array[index + 3] = (byte)((temp >>>  0) & 0xFF);
	}
	
	public static void putInteger(byte[] array, int index, int value) {
		array[index + 0] = (byte)(value >>> 24);
		array[index + 1] = (byte)(value >>> 16);
		array[index + 2] = (byte)(value >>> 8);
		array[index + 3] = (byte)(value >>> 0);
	}
	
	public static void putShort(byte[] array, int index, short value) {
		array[index + 0] = (byte)(((int)value >>> 8) & 0xFF);
		array[index + 1] = (byte)(((int)value >>> 0) & 0xFF);
	}
	
	public static void putBoolean(byte[] array, int index, boolean b) {
		array[index] = (byte) (b ? 1 : 0);
	}
	
	/**
	 * Compressed byte data, shows no exta memory savings after two compressions
	 * @param data the byte array to compress
	 * @param times the number of times to compress the data, two times is ideal
	 * @return a new compressed byte array
	 * @throws IOException
	 */
	public static byte[] compress(byte[] data, int times) throws IOException {
		if(times > 0) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DeflaterOutputStream deflater = new DeflaterOutputStream(out);
			deflater.write(data);
			deflater.close();
			return compress(out.toByteArray(), times - 1);
		} else {
			return data;
		}
	}
	
	/**
	 * Serialized an object into a byte array.
	 * Format: [4 bytes : length of class name string] + [class name] + [4 bytes : object data length] + [object data]
	 * @param object
	 * @return
	 */
	public static byte[] serialize(Transportable object) {
		byte[] nameBytes;
		byte[] objectBytes;
		if(object == null) {
			nameBytes = "null".getBytes();
			objectBytes = new byte[0];
		} else {
			nameBytes = object.getClass().getName().getBytes();
			objectBytes = object.toBytes();
		}
		
		//class name length, class name, object data length, object data
		byte[] data = new byte[4 + nameBytes.length + 4 + objectBytes.length];
		
		//first four bytes are the length of the class name
		putInteger(data, 0, nameBytes.length);
		
		//put the class name in the following bytes
		System.arraycopy(nameBytes, 0, data, 4, nameBytes.length);
		
		//put the length of the object data in the next four bytes after class name
		putInteger(data, 4 + nameBytes.length, objectBytes.length);
		
		//put the object data into the final byte array
		System.arraycopy(objectBytes, 0, data, 4 + nameBytes.length + 4, objectBytes.length);
		
		return data;
	}
	
	public static byte[] serializeHeaderless(Transportable object) {
		return object.toBytes();
	}
}
