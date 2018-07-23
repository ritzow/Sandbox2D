package ritzow.sandbox.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class SerializerReaderWriter implements Serializer, Deserializer {
	private final Map<Short, Function<TransportableDataReader, ? extends Transportable>> deserializeLookup;
	private final Map<Class<? extends Transportable>, Short> serializeLookup;
	
	public SerializerReaderWriter() {
		this.deserializeLookup = new HashMap<>();
		this.serializeLookup = new HashMap<>();
	}

	public <T extends Transportable> SerializerReaderWriter 
		register(short identifier, Class<T> returnType, Function<TransportableDataReader, T> deserializer) {
		deserializeLookup.put(identifier, deserializer);
		serializeLookup.put(returnType, identifier);
		return this;
	}
	
	public void register(short identifier, Consumer<TransportableDataReader> deserializer) {
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
		Bytes.putInteger(data, 0, 2 + objectBytes.length);
		
		//put the short value representing the type of object serialized after the data length
		Bytes.putShort(data, 4, typeID);
		
		//put the object data into the final byte array
		Bytes.copy(objectBytes, data, 6);
		
		return data;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T deserialize(byte[] object) throws ClassNotRegisteredException { //TODO add inputstream version
		//if the object length is 0, the object is null
		int length = Bytes.getInteger(object, 0);
		
		if(length == 0) {
			return null;	
		}
		
		//get type, associated with a method to deserialize the object
		short type = Bytes.getShort(object, 4);
		
		//get the concrete class of the object
		Function<TransportableDataReader, ? extends Transportable> func = deserializeLookup.get(type);
		
		//throw exception if not registered
		if(func == null)
			throw new ClassNotRegisteredException("Cannot deserialize unregistered class of type " + type);
		return (T)func.apply(getReader(object));
	}
	
	private TransportableDataReader getReader(final byte[] bytes) { //for use by objects you want to deserialize.
		return new TransportableDataReader() {
			private int index = 6; //skip past object size and type
			
			public int remaining() {
				return Bytes.getInteger(bytes, index - 4) - index + 4; //TODO is this implemented correctly?
			}
			
			public void skip(int bytes) {
				checkIndex(index += bytes);
			}
			
			private void checkIndex(int index) {
				if(index >= bytes.length)
					throw new IndexOutOfBoundsException("not enough data remaining");
			}
			
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
				long value = 0;
				for (int i = 0; i < 8; i++) {
					value = (value << 8) + (readByte() & 0xff);
				}
				return value;
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
			public byte[] readBytes(int count) {
				if(bytes.length < index + count || count < 0)
					throw new IndexOutOfBoundsException("not enough data remaining");
				byte[] data = new byte[count];
				System.arraycopy(bytes, index, data, 0, data.length);
				index += count;
				return data;
			}
			
			@Override
			public void readBytes(byte[] dest, int offset) {
				if(dest.length < bytes.length - index) {
					System.arraycopy(bytes, index, dest, offset, dest.length - offset);
				} else {
					throw new IndexOutOfBoundsException("not enough data remaining");
				}
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T extends Transportable> T readObject() {
				int length = readInteger(); //read length of data
				if(length == 0) //length of 0 represents null
					return null;
				int beginIndex = index; //store the starting index for error checking later
				short type = readShort(); //read type
				Function<TransportableDataReader, ? extends Transportable> func = deserializeLookup.get(type); //get associated function
				if(func == null)
					throw new ClassNotRegisteredException("Cannot deserialize unregistered class of type " + type);
				try {
					T object = (T)func.apply(this);
					return checkSize(length, index - beginIndex, object);
				} catch(IndexOutOfBoundsException e) {
					throw new SerializationException("object read too many bytes and reached the end of the data");
				}
			}
			
			private final <T> T checkSize(int expectedLength, int lengthRead, T object) {
				if(lengthRead != expectedLength) {
					StringBuilder sb = new StringBuilder("object of type ")
							.append(object.getClass().getName())
							.append(" read ")
							.append(Math.abs(lengthRead - expectedLength))
							.append(" bytes ")
							.append(lengthRead > expectedLength ? "too many" : "too few");
					throw new SerializationException(sb.toString());
				}
				return object;
			}
		};
	}
}
