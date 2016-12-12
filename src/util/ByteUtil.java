package util;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.nustaq.serialization.FSTConfiguration;

public final class ByteUtil {
	
	private static final FSTConfiguration serializer;
	
	static {
		serializer = FSTConfiguration.createDefaultConfiguration();
		serializer.registerClass(
				world.block.Block.class, 
				world.block.DirtBlock.class, 
				world.block.GrassBlock.class, 
				world.block.RedBlock.class, 
				world.entity.Entity.class,
				world.entity.ItemEntity.class,
				world.entity.BlockPlaceParticleEntity.class,
				world.entity.LivingEntity.class,
				world.entity.ParticleEntity.class,
				world.entity.Player.class
		);
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
		ObjectInputStream deserializer = new ObjectInputStream(inflater);
		Object object = deserializer.readObject();
		deserializer.close();
		return object;
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
		out.close();
		return out.toByteArray();
	}
	
	public static byte[] serializeCompressed(Object object) throws IOException { //TODO use FST Serialization?
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DeflaterOutputStream compresser = new DeflaterOutputStream(out);
		ObjectOutputStream serializer = new ObjectOutputStream(compresser);
		serializer.writeObject(object);
		serializer.close();
		return out.toByteArray();
	}
	
//	private static final class ByteArrayFillOutputStream extends OutputStream {
//		protected byte[] array;
//		protected int index;
//		
//		public ByteArrayFillOutputStream(byte[] array, int offset) {
//			this.array = array;
//			this.index = offset;
//		}
//
//		@Override
//		public void write(int b) throws IOException {
//			if(index >= array.length)
//				throw new ArrayIndexOutOfBoundsException();
//			array[index] = (byte)b;
//			index++;
//		}
//
//		@Override
//		public void write(byte[] b, int off, int len) throws IOException {
//			if(index >= array.length)
//				throw new ArrayIndexOutOfBoundsException();
//			System.arraycopy(b, off, array, index, len);
//			index += len;
//		}
//	}
}
