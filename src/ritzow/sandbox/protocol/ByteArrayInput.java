package ritzow.sandbox.protocol;

import java.io.DataInput;
import java.io.IOException;
import java.util.Objects;

public class ByteArrayInput implements DataInput {
	private final byte[] data;
	private int index;
	
	public ByteArrayInput(byte[] data) {
		this.data = data;
	}
	
	@Override
	public void readFully(byte[] b) throws IOException {
		readFully(b, 0, b.length);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		Objects.requireNonNull(b);
		if(off + len > b.length)
			throw new IndexOutOfBoundsException("Destination array not large enough");
		System.arraycopy(data, 0, b, off, len);
		index += len;
		if(len > data.length)
			throw new IOException("No more bytes to read");
		
	}

	@Override
	public int skipBytes(int n) throws IOException {
		int skipped = Math.min(n, data.length - index);
		index += skipped;
		return skipped;
	}

	@Override
	public boolean readBoolean() throws IOException {
		return readByte() == 1;
	}

	@Override
	public byte readByte() throws IOException {
		if(index == data.length)
			throw new IndexOutOfBoundsException("No more bytes to read");
		return data[++index];
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return Byte.toUnsignedInt(readByte());
	}

	@Override
	public short readShort() throws IOException {
		return (short)((readByte() << 8) | (readByte() & 0xff));
	}

	@Override
	public int readUnsignedShort() throws IOException {
		return Short.toUnsignedInt(readShort());
	}

	@Override
	public char readChar() throws IOException {
		return (char)readUnsignedShort();
	}

	@Override
	public int readInt() throws IOException {
		return ((readByte() & 255) << 24) | ((readByte() & 255) << 16) | ((readByte() & 255) << 8) | ((readByte() & 255) << 0);
	}

	@Override
	public long readLong() throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public float readFloat() throws IOException {
		return Float.intBitsToFloat(((readByte() & 255) << 24) | ((readByte() & 255) << 16) | ((readByte() & 255) << 8) | ((readByte() & 255) << 0));
	}

	@Override
	public double readDouble() throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public String readLine() throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public String readUTF() throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

}
