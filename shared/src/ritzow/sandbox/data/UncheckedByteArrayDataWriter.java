package ritzow.sandbox.data;

public class UncheckedByteArrayDataWriter implements DataWriter {
	private final byte[] dest;
	private int index;
	
	public UncheckedByteArrayDataWriter(byte[] dest, int offset) {
		this.dest = dest;
		this.index = offset;
	}
	
	public int index() {
		return index;
	}

	@Override
	public void writeDouble(double value) {
		Bytes.putDouble(dest, index, value);
		index += 8;
	}

	@Override
	public void writeFloat(float value) {
		Bytes.putFloat(dest, index, value);
		index += 4;
	}

	@Override
	public void writeLong(long value) {
		Bytes.putLong(dest, index, value);
		index += 8;
	}

	@Override
	public void writeInteger(int value) {
		Bytes.putInteger(dest, index, value);
		index += 4;
	}

	@Override
	public void writeShort(short value) {
		Bytes.putShort(dest, index, value);
		index += 2;
	}

	@Override
	public void writeBoolean(boolean value) {
		Bytes.putBoolean(dest, index, value);
		index++;
	}

	@Override
	public void writeByte(byte value) {
		dest[index] = value;
		index++;
	}

	@Override
	public void writeBytes(byte[] data) {
		writeBytes(data, 0, data.length);
	}

	@Override
	public void writeBytes(byte[] data, int offset, int length) {
		System.arraycopy(data, offset, dest, index, length);
		index += length;
	}

}
