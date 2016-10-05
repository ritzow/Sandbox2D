package audio;

import java.io.IOException;
import java.io.InputStream;

public final class WaveformReader {
	
	private InputStream input;
	
	private int chunkSize;
	
	private int format; //1 for PCM audio, other numbers indicate compression
	private int numChannels; //1 = mono, 2 = stereo
	private int sampleRate; //8000, 44100
	private int byteRate; //sampleRate * numChannels * bitsPerSample/8
	private int blockAlign; //numChannels * bitsPerSample/8
	private int bitsPerSample; //8 bits = 8, 16 bits = 16, etc.
	
	public WaveformReader(InputStream input) {
		this.input = input;
	}
	
	public void decode() throws IOException {
		checkHeader();
		readFormat();
	}
	
	public void printInfo() {
		System.out.println("ChunkSize: " + chunkSize);
		System.out.println("Format: " + format);
		System.out.println("Channels: " + numChannels);
		System.out.println("Sample Rate:"  + sampleRate);
		System.out.println("Byte Rate:" + byteRate);
		System.out.println("Block Align: " + blockAlign);
		System.out.println("Bits per sample: " + bitsPerSample);
	}
	
	private void checkHeader() throws IOException {
		if(!readStringBigEndian(4).equals("RIFF"))
			throw new IOException("file does not begin with the RIFF header");
		
		chunkSize = readIntegerLittleEndian();
		
		byte[] format = new byte[4]; input.read(format, 0, 4);
		if(!(new String(format).equals("WAVE")))
			throw new IOException("file format is not WAVE");
	}
	
	private void readFormat() throws IOException {
		if(!readStringBigEndian(4).equals("fmt "))
			throw new IOException("chunk id not fmt ");
		
		skipBytes(4); //skip Subchunk1Size
		
		format = readShortLittleEndian(); //read AudioFormat section
		
	}
	
	private void skipBytes(int numBytes) throws IOException {
		while(numBytes > 0) {
			input.read();
		}
	}
	
	private String readStringBigEndian(int numBytes) throws IOException {
		byte[] data = new byte[numBytes]; input.read(data); return new String(data);
	}
	
	private short readShortLittleEndian() throws IOException { //TODO this probably doesnt work correctly
		byte[] data = new byte[2]; input.read(data);
		return (short) (((data[1] & 0xFF) << 8) | (data[0] & 0xFF));
	}
	
	private int readIntegerBigEndian() throws IOException {
		return (((input.read() & 0xff) << 24) | ((input.read() & 0xff) << 16) | ((input.read() & 0xff) << 8) | (input.read() & 0xff));
	}
	
	private int readIntegerLittleEndian() throws IOException {
		byte[] data = new byte[4]; input.read(data);
		return (((data[3] & 0xff) << 24) | ((data[2] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[0] & 0xff));
	}
}
