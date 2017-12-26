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
		ByteUtil.putDouble(dest, index, value);
		index += 8;
	}

	@Override
	public void writeFloat(float value) {
		ByteUtil.putFloat(dest, index, value);
		index += 4;
	}

	@Override
	public void writeLong(long value) {
		ByteUtil.putLong(dest, index, value);
		index += 8;
	}

	@Override
	public void writeInteger(int value) {
		ByteUtil.putInteger(dest, index, value);
		index += 4;
	}

	@Override
	public void writeShort(short value) {
		ByteUtil.putShort(dest, index, value);
		index += 2;
	}

	@Override
	public void writeBoolean(boolean value) {
		ByteUtil.putBoolean(dest, index, value);
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
