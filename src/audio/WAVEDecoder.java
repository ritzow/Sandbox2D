package audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

public final class WAVEDecoder {
	
	private InputStream input;
	
	private short format; //1 for PCM audio, other numbers indicate compression
	private short numChannels; //1 = mono, 2 = stereo
	private int sampleRate; //8000, 44100
	private int byteRate; //sampleRate * numChannels * bitsPerSample/8
	private short blockAlign; //numChannels * bitsPerSample/8
	private short bitsPerSample; //8 bits = 8, 16 bits = 16, etc.
	
	private ByteBuffer data;
	
	public WAVEDecoder(InputStream input) {
		this.input = input;
	}
	
	public final short getFormat() {
		return format;
	}

	public final short getChannels() {
		return numChannels;
	}

	public final int getSampleRate() {
		return sampleRate;
	}

	public final int getByteRate() {
		return byteRate;
	}

	public final short getBlockAlign() {
		return blockAlign;
	}

	public final short getBitsPerSample() {
		return bitsPerSample;
	}
	
	public void decode() throws IOException {
		checkHeader();
		findChunk("fmt ");
		readFormat();
		findChunk("data");
		readData();
		data.flip();
	}

	public void close() throws IOException {
		input.close();
	}

	public String toString() {
		return "Format: " + format + "\nChannels: " + numChannels + "\nSample Rate: " + sampleRate + "\nByte Rate: "  + byteRate + "\nBlock Align: " + blockAlign + "\nBits per sample: " + bitsPerSample;
	}
	
	public ByteBuffer getData() {
		return data;
	}
	
	private void checkHeader() throws IOException {
		if(!readStringBigEndian(4).equals("RIFF"))
			throw new IOException("file does not begin with the RIFF header");
		
		input.skip(4); //skip ChunkSize for RIFF header
		
		byte[] format = new byte[4]; input.read(format, 0, 4);
		if(!(new String(format).equals("WAVE")))
			throw new IOException("file format is not WAVE");
	}
	
	private void findChunk(String chunkID) throws IOException {
		if(!readStringBigEndian(chunkID.getBytes().length).equals(chunkID)) {
			int chunkSize = readIntegerLittleEndian();
			input.skip(chunkSize);
			
			if(input.available() == 0)
				throw new IOException("Could not find '" + chunkID + "' chunk");
			else
				findChunk(chunkID);
		}
	}
	
	private void readFormat() throws IOException {
		input.skip(4); //skip subchunk1size for now
		
		if(readShortLittleEndian() != 1) //read the format, 1 for PCM
			throw new IOException("audio format is not PCM data");
		
		numChannels = readShortLittleEndian(); //read number of channels (1 or 2)
		sampleRate = readIntegerLittleEndian(); //read sample rate (ie 44100 44100 Hz)
		byteRate = readIntegerLittleEndian(); //read byte rate (SampleRate * NumChannels * BitsPerSample/8)
		blockAlign = readShortLittleEndian(); //read block align (NumChannels * BitsPerSample/8)
		bitsPerSample = readShortLittleEndian(); //bits per sample 8 for 8 bits, 16 for 16 bits, etc.
	}
	
	private void readData() throws IOException {		
		int dataSize = readIntegerLittleEndian(); //SubChunk2Size (NumSamples * NumChannels * BitsPerSample/8) aka the amount of space I need to store data
		data = BufferUtils.createByteBuffer(dataSize);
		
		while(data.hasRemaining())
			data.put((byte)input.read()); //WAVE data is interleaved left/right sample, each sample is bits per sample large
	}
	
	private String readStringBigEndian(int numBytes) throws IOException {
		byte[] data = new byte[numBytes]; input.read(data); return new String(data);
	}
	
	private short readShortLittleEndian() throws IOException {
		byte[] data = new byte[2]; input.read(data);
		return (short) (((data[1] & 0xFF) << 8) | (data[0] & 0xFF));
	}
	
	private int readIntegerLittleEndian() throws IOException {
		byte[] data = new byte[4]; input.read(data);
		return (((data[3] & 0xff) << 24) | ((data[2] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[0] & 0xff));
	}
}
