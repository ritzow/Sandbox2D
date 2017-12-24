package ritzow.sandbox.data;

public class ByteArrayDataReader implements DataReader {
	private final byte[] bytes;
	private int index;
	
	public ByteArrayDataReader(byte[] data) {
		bytes = data;
	}
	
	public int remaining() {
		return bytes.length - index;
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
}
