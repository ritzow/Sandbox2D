package ritzow.sandbox.data;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SerializerReaderWriter implements Serializer, Deserializer {
	private final Map<Short, ObjectBuilder> deserializeLookup;
	private final Map<Class<? extends Transportable>, Short> serializeLookup;

	public SerializerReaderWriter() {
		this.deserializeLookup = new HashMap<Short, ObjectBuilder>();
		this.serializeLookup = new HashMap<Class<? extends Transportable>, Short>();
	}

	public <T extends Transportable> SerializerReaderWriter
		register(short identifier, Class<T> returnType, ObjectBuilder deserializer) {
		registerWrite(identifier, returnType);
		registerRead(identifier, deserializer);
		return this;
	}

	public <T extends Transportable> SerializerReaderWriter registerWrite(short identifier, Class<T> returnType) {
		if(identifier == 0)
			throw new IllegalArgumentException("0 is a reserved identifier");
		if(serializeLookup.putIfAbsent(Objects.requireNonNull(returnType), identifier) != null) {
			throw new IllegalArgumentException(returnType + " already has an identifier");
		}
		return this;
	}

	public SerializerReaderWriter registerRead(short identifier, ObjectBuilder deserializer) {
		if (deserializeLookup.putIfAbsent(identifier, Objects.requireNonNull(deserializer)) != null) {
			throw new IllegalArgumentException(identifier + " already has a deserializer");
		}
		return this;
	}

	@Override
	public byte[] serialize(Transportable object) {
		//null objects have 0 length
		if(object == null)
			return new byte[4]; //init to zero

		Short typeID = serializeLookup.get(object.getClass());
		if(typeID == null)
			throw new TypeNotRegisteredException("Class " + object.getClass().getName() + " is not registered");
		byte[] objectBytes = object.getBytes(this);

		int objectLength = objectBytes.length + 2;
		//serialized object length, typeID, object data
		byte[] data = new byte[4 + objectLength];

		//first four bytes are the length of the rest of the data (INCLUDING THE TYPE)
		Bytes.putInteger(data, 0, objectLength);

		//put the short value representing the type of object serialized after the data length
		Bytes.putShort(data, 4, typeID);

		//put the object data into the final byte array
		Bytes.copy(objectBytes, data, 6);

		return data;
	}

	@SuppressWarnings("unchecked")
	public <T> T deserialize(ByteBuffer buffer) throws TypeNotRegisteredException {
		return getReader(buffer).readObject();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T deserialize(byte[] object) throws TypeNotRegisteredException { //TODO add inputstream version
		return getReader(object).readObject();
	}

	private TransportableDataReader getReader(final ByteBuffer bytes) {
		return new AbstractDataReader() {
			@Override
			public int remaining() {
				return bytes.remaining();
			}

			@Override
			public void skip(int countBytes) {
				bytes.position(bytes.position() + countBytes);
			}

			@Override
			public byte readByte() {
				return bytes.get();
			}

			@Override
			public byte[] readBytes(int count) {
				if(count > bytes.remaining())
					throw new IllegalArgumentException("Not enough bytes remaining.");
				byte[] dst = new byte[count];
				bytes.get(dst);
				return dst;
			}

			@Override
			public void readBytes(byte[] dest, int offset) {
				bytes.get(dest, offset, dest.length);
			}
		};
	}

	private TransportableDataReader getReader(final byte[] bytes) {
		return new AbstractDataReader() {
			private int index = 0; //skip past object size and type

			@Override
			public int remaining() {
				return bytes.length - index; //TODO is this implemented correctly?
			}

			@Override
			public void skip(int bytes) {
				checkIndex(index += bytes);
			}

			private void checkIndex(int index) {
				if(index >= bytes.length)
					throw new IndexOutOfBoundsException("not enough data remaining");
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
		};
	}

	private abstract class AbstractDataReader implements TransportableDataReader {
		@Override
		public abstract byte readByte();
		@Override
		public abstract byte[] readBytes(int count);
		@Override
		public abstract void readBytes(byte[] dest, int offset);
		@Override
		public abstract int remaining();
		@Override
		public abstract void skip(int bytes);

		@Override
		@SuppressWarnings("unchecked")
		public <T extends Transportable> T readObject() {
			try {
				int length = readInteger();
				if(length == 0) { //length of 0 represents null
					return null;
				} else {
					int rem = remaining();
					short type = readShort();
					//System.out.println("Reading object of type " + type + " with length " + length);
					ObjectBuilder func = deserializeLookup.get(type); //get associated function
					if(func == null) throw new TypeNotRegisteredException(
						"Cannot deserialize unregistered type " + type);
					T obj = (T)func.apply(this);
					int bytesRead = rem - remaining();
					if(bytesRead != length)
						throw new SerializationException("Read "
							+ (bytesRead > length ? (bytesRead - length)
							+ " bytes too many" : (length - bytesRead) + " bytes too few")
							+ " while deserializing type " + type + " of length " + length + ".");
					return obj;
				}
			} catch(IndexOutOfBoundsException | BufferUnderflowException e) {
				throw new SerializationException("object read too many bytes and reached the end of the data");
			}
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
			return
				(readByte() << 24) |
				((readByte() & 255) << 16) |
				((readByte() & 255) << 8) |
				((readByte() & 255));
		}

		@Override
		public short readShort() {
			return (short)(((readByte() & 255) << 8) | ((readByte() & 255)));
		}

		@Override
		public boolean readBoolean() {
			return readByte() == 1;
		}

		@Override
		public boolean[] readCompactBooleans() {
			byte eightbools = readByte();
			boolean[] booleans = new boolean[8];
			for (int i = 0; i < 8; i++) {
				booleans[i] = ((eightbools >> i) & 1) == 1;
			}
			return booleans;
		}

		private <T> T checkSize(int expectedLength, int lengthRead, T object) {
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
	}
}
