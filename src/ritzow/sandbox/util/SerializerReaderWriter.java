package ritzow.sandbox.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class SerializerReaderWriter implements Serializer, Deserializer {
	private final Map<Short, Function<DataReader, ? extends Transportable>> deserializeLookup;
	private final Map<Class<? extends Transportable>, Short> serializeLookup;
	
	public SerializerReaderWriter() {
		this.deserializeLookup = new HashMap<>();
		this.serializeLookup = new HashMap<>();
	}

	public <T extends Transportable> void register(short identifier, Class<T> returnType, Function<DataReader, T> deserializer) {
		deserializeLookup.put(identifier, deserializer);
		serializeLookup.put(returnType, identifier);
	}
	
	public void register(short identifier, Consumer<DataReader> deserializer) {
		if(identifier == 0)
			throw new IllegalArgumentException("identifier 0 reserved for null values");
		
		//when the function has no return type, this method will take a consumer and return null
		deserializeLookup.put(identifier, in -> {deserializer.accept(in); return null;});
	}
	
	@Override
	public byte[] serialize(Transportable object) {
		//null objects have 0 length
		if(object == null)
			return new byte[4]; //init to zero
		
		Short typeID = serializeLookup.get(object.getClass());
		if(typeID == null)
			throw new ClassNotRegisteredException("Class " + object.getClass().getName() + " is not registered");
		
		byte[] objectBytes = object.getBytes(this);

		//serialized object length, typeID, object data
		byte[] data = new byte[4 + 2 + objectBytes.length];
		
		//first four bytes are the length of the rest of the data (INCLUDING THE TYPE)
		ByteUtil.putInteger(data, 0, 2 + objectBytes.length);
		
		//put the short value representing the type of object serialized after the data length
		ByteUtil.putShort(data, 4, typeID);
		
		//put the object data into the final byte array
		ByteUtil.copy(objectBytes, data, 6);
		
		return data;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T deserialize(byte[] object) {
		//if the object length is 0, the object is null
		int length = ByteUtil.getInteger(object, 0);
		
		if(length == 0) {
			return null;	
		} else {
			//get type, associated with a method to deserialize the object
			short type = ByteUtil.getShort(object, 4);
			
			Function<DataReader, ? extends Transportable> func = deserializeLookup.get(type); //get the concrete class of the object
			if(func == null)
				throw new ClassNotRegisteredException("Cannot deserialize unregistered class of type " + type);
			return (T)func.apply(getReader(object));
		}
	}
	
	private DataReader getReader(byte[] data) { //for use by objects you want to deserialize.
		return new DataReader() {
			private final byte[] bytes = data;
			private int index = 6; //skip past object size and type
			
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
				return (readByte() << 24) | ((readByte() & 255) << 16) | ((readByte() & 255) << 8) | ((readByte() & 255) << 0);
			}

			@Override
			public short readShort() {
				return (short)(((readByte() & 255) << 8) | ((readByte() & 255) << 0));
			}
			
			@Override
			public boolean readBoolean() {
				return readByte() == 1;
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
					throw new IndexOutOfBoundsException("not enough data remaining");
				byte[] data = new byte[bytes];
				System.arraycopy(this.bytes, index, data, 0, data.length);
				index += bytes;
				return data;
			}
			
			@Override
			public void readBytes(byte[] dest, int offset) {
				if(dest.length < bytes.length - index) {
					System.arraycopy(bytes, index, dest, offset, dest.length - offset);
				} else {
					throw new ArrayIndexOutOfBoundsException("not enough data remaining");
				}
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T> T readObject() {
				//int startIndex = index;
				int length = readInteger(); //read length of data TODO for some reason it is sometimes negative!?
				if(length == 0)
					return null; //length of 0 represents null
				short type = readShort(); //read type
				Function<DataReader, ? extends Transportable> func = deserializeLookup.get(type); //get associated function
				if(func == null)
					throw new ClassNotRegisteredException("Cannot deserialize unregistered class of type " + type);
				T object = (T)func.apply(this);
				//if(index > startIndex + length)
				//	throw new IndexOutOfBoundsException("read too much data");
				return object;
			}
		};
	}
}
