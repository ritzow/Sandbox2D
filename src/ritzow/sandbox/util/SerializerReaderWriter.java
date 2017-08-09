package ritzow.sandbox.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class SerializerReaderWriter {
	private final Map<Short, Function<DataReader, ? extends Transportable>> classlookup;
	private final Map<Class<? extends Transportable>, Short> idLookup;
	
	public SerializerReaderWriter() {
		this.classlookup = new HashMap<>();
		this.idLookup = new HashMap<>();
	}

	public <T extends Transportable> void register(short identifier, Class<T> returnType, Function<DataReader, T> deserializer) {
		classlookup.put(identifier, deserializer);
		idLookup.put(returnType, identifier);
	}
	
	public DataReader getReader(byte[] data) {
		return new DataReader() {
			private final byte[] bytes = data;
			private int index = 0;
			
			@Override
			public double readDouble() {
				return Double.longBitsToDouble(readLong());
			}

			@Override
			public float readFloat() {
				return Float.intBitsToFloat(readInteger());
			}

			@Override
			public long readLong() {
				throw new UnsupportedOperationException("readLong not implemented");
			}

			@Override
			public int readInteger() {
				return (readByte() << 24) | (readByte() << 16) | (readByte() << 8) | (readByte() << 0);
			}

			@Override
			public short readShort() {
				return (short)((readByte() << 8) | (readByte() << 0)); //TODO is something wrong with readShort?
			}
			
			@Override
			public boolean readBoolean() {
				return readByte() == 1 ? true : false;
			}
			
			@Override
			public boolean[] readCompactBooleans() {
				byte eightbools = readByte();
				boolean[] booleans = new boolean[8];
				for(int i = 0; i < 8; i++) {
					booleans[i] = ((eightbools >> i) & 1) == 1;
				}
				return booleans;
			}

			@Override
			public byte readByte() {
				return bytes[index++];
			}

			@Override
			public byte[] readBytes(int bytes) {
				if(this.bytes.length < bytes || bytes < 0)
					throw new ArrayIndexOutOfBoundsException("not enough data remaining");
				byte[] data = new byte[bytes];
				System.arraycopy(this.bytes, index, data, 0, data.length);
				index += bytes;
				return data;
			}
			
			public void readBytes(byte[] dest, int offset) {
				if(dest.length < bytes.length - index) {
					System.arraycopy(bytes, index, dest, offset, dest.length - offset);
				} else {
					throw new ArrayIndexOutOfBoundsException("not enough data remaining");
				}
			}

			@SuppressWarnings("unchecked")
			@Override
			public <T> T readObject() {
				int currentIndex = index;
				int length = readInteger(); //read length of data
				short type = readShort();
				Function<DataReader, ? extends Transportable> func = classlookup.get(type);
				if(func == null)
					throw new ClassNotRegisteredException("Cannot deserialize unregistered class for type " + type);
				T object = (T)func.apply(this);
				if(index > currentIndex + length)
					throw new RuntimeException("Deserializer function" + func.getClass() + " read too much data");
				return object;
			}	
		};
	}
	
	@SuppressWarnings("unchecked")
	public <T> T deserialize(byte[] object) throws ReflectiveOperationException {
		Function<DataReader, ? extends Transportable> func = classlookup.get(ByteUtil.getShort(object, 4)); //get the concrete class of the object
		if(func == null)
			throw new ClassNotRegisteredException("Cannot deserialize unregistered class");
		return (T) func.apply(getReader(object));
	}
	
	public byte[] serialize(Transportable object) {
		Objects.requireNonNull(object);
		
		Short typeID = idLookup.get(object.getClass());
		if(typeID == null)
			throw new ClassNotRegisteredException("Class " + object.getClass().getName() + " is not registered");
		
		byte[] objectBytes = object.getBytes();

		//class name length, class name, object data length, object data
		byte[] data = new byte[4 + 2 + objectBytes.length];
		
		//first four bytes are the length of the rest of the data
		ByteUtil.putInteger(data, 0, 2 + objectBytes.length);
		
		//put the short value representing the type of object serialized after the data length
		ByteUtil.putShort(data, 4, typeID);
		
		//put the object data into the final byte array
		ByteUtil.copy(objectBytes, data, 6);
		
		return data;
	}
}
