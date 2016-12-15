package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

public final class ByteUtil {
	
	private static final FSTConfiguration fstConfig;
	
	static {
		fstConfig = FSTConfiguration.createDefaultConfiguration();
//		fstConfig.registerClass(
//				world.block.Block.class, 
//				world.block.DirtBlock.class, 
//				world.block.GrassBlock.class, 
//				world.block.RedBlock.class, 
//				world.entity.Entity.class,
//				world.entity.ItemEntity.class,
//				world.entity.BlockPlaceParticleEntity.class,
//				world.entity.LivingEntity.class,
//				world.entity.ParticleEntity.class,
//				world.entity.Player.class,
//				world.entity.component.Inventory.class,
//				world.BlockGrid.class,
//				world.World.class,
//				world.item.BlockItem.class
//		);
	}
	
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
	
	//Data composition from bytes
	
	public static short getShort(byte[] array, int index) {
		return (short)((array[index] << 8) | (array[index + 1] & 0xff));
	}
	
	public static int getInteger(byte[] array, int index) {
		return ((array[index] & 255) << 24) | ((array[index + 1] & 255) << 16) | ((array[index + 2] & 255) << 8) | ((array[index + 3] & 255) << 0);
	}
	
	private static float getFloat(byte a, byte b, byte c, byte d) {
		return Float.intBitsToFloat(((a & 255) << 24) | ((b & 255) << 16) | ((c & 255) << 8) | ((d & 255) << 0));
	}
	
	public static float getFloat(byte[] array, int index) {
		return getFloat(array[index], array[index + 1], array[index + 2], array[index + 3]);
	}
	
	public static boolean getBoolean(byte[] array, int index) {
		return array[index] == 1 ? true : false;
	}
	
	public static Object deserialize(byte[] data) throws ClassNotFoundException, IOException {
		return deserialize(data, 0, data.length);
	}
	
	public static Object deserialize(byte[] data, int offset, int length) throws ClassNotFoundException, IOException {
		return new ObjectInputStream(new ByteArrayInputStream(data, offset, length)).readObject();
	}
	
	public static Object deserializeCompressed(byte[] data) throws ClassNotFoundException, IOException {
		return deserializeCompressed(data, 0, data.length);
	}
	
	public static Object deserializeCompressed(byte[] data, int offset, int length) throws ClassNotFoundException, IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(data, offset, length);
		InflaterInputStream inflater = new InflaterInputStream(in);
		FSTObjectInput deserializer = fstConfig.getObjectInput(inflater);
		Object object = deserializer.readObject();
		inflater.close();
		return object;
	}
	
	/**
	 * 
	 * @param object the object packet, where the first four bytes are an integer representing the length of the class name String, followed by the class name
	 * @param offset
	 * @param length
	 * @return
	 * @throws ReflectiveOperationException
	 */
	public static <T extends Transportable> T deserializeTransportable(byte[] object) throws ReflectiveOperationException {
		int classNameLength = ByteUtil.getInteger(object, 0);
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>)(Class.forName(new String(object, 4, classNameLength)));
		return deserializeTransportable(clazz, Arrays.copyOfRange(object, classNameLength + 4, object.length));
	}
	
	public static <T extends Transportable> T deserializeTransportable(Class<T> type, byte[] data) throws ReflectiveOperationException {
		return type.getConstructor(byte[].class).newInstance(data);
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
	
	public static byte[] serialize(Object object) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream serializer = new ObjectOutputStream(out);
		serializer.writeObject(object);
		serializer.close();
		return out.toByteArray();
	}
	
	public static byte[] serializeCompressed(Object object) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DeflaterOutputStream compresser = new DeflaterOutputStream(out);
		FSTObjectOutput serializer = fstConfig.getObjectOutput(compresser);
		serializer.writeObject(object);
		serializer.flush();
		compresser.close();
		return out.toByteArray();
	}
	
	public static byte[] serializeTransportable(Transportable object) {
		String name = object.getClass().getName();
		byte[] nameBytes = name.getBytes();
		byte[] objectBytes = object.toBytes();
		byte[] packet = new byte[4 + nameBytes.length + objectBytes.length];
		ByteUtil.putInteger(packet, 0, nameBytes.length);
		System.arraycopy(nameBytes, 0, packet, 4, nameBytes.length);
		System.arraycopy(objectBytes, 0, packet, nameBytes.length + 4, objectBytes.length);
		return packet;
	}
	
	public static byte[] serializeTransportableHeaderless(Transportable object) {
		return object.toBytes();
	}
}
