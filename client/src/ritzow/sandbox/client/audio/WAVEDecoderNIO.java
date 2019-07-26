package ritzow.sandbox.client.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.lwjgl.BufferUtils;

public class WAVEDecoderNIO implements SoundInfo {
	private short format; //1 for PCM audio, other numbers indicate compression
	private short numChannels; //1 = mono, 2 = stereo
	private int sampleRate; //8000, 44100
	private int byteRate; //sampleRate * numChannels * bitsPerSample/8
	private short blockAlign; //numChannels * bitsPerSample/8
	private short bitsPerSample; //8 bits = 8, 16 bits = 16, etc.
	private ByteBuffer data;

	public static SoundInfo decode(Path file) throws IOException {
		try(FileChannel reader = FileChannel.open(file, StandardOpenOption.READ)) {
			ByteBuffer buffer = ByteBuffer.allocate((int)reader.size()).order(ByteOrder.LITTLE_ENDIAN);
			reader.read(buffer);
			buffer.flip();
			checkHeader(buffer);
			WAVEDecoderNIO info = new WAVEDecoderNIO();
			findChunk("fmt ", buffer);
			readFormat(buffer, info);
			findChunk("data", buffer);
			readData(buffer, info);
			return info;
		}
	}
	
	public short format() {
		return format;
	}
	
	public int byteRate() {
		return byteRate;
	}
	
	public short blockAlign() {
		return blockAlign;
	}
	
	@Override
	public short getBitsPerSample() {
		return bitsPerSample;
	}

	@Override
	public boolean isSigned() {
		return getBitsPerSample() == 16;
	}

	@Override
	public short getChannels() {
		return numChannels;
	}

	@Override
	public int getSampleRate() {
		return sampleRate;
	}

	@Override
	public ByteBuffer getData() {
		checkStatus();
		return data;
	}
	
	private void checkStatus() {
		if(data == null)
			throw new RuntimeException("file has not been decoded yet");
	}
	
	private static void checkHeader(ByteBuffer data) throws IOException {
		if(!readString(data).equals("RIFF"))
			throw new IOException("file does not begin with the RIFF header");
		data.position(data.position() + 4); //skip ChunkSize for RIFF header
		if(!readString(data).equals("WAVE"))
			throw new IOException("file format is not WAVE");
	}
	
	private static String readString(ByteBuffer data) {
		byte[] header = new byte[4];
		data.get(header);
		return new String(header, StandardCharsets.US_ASCII);
	}
	
	private static void findChunk(String chunkID, ByteBuffer data) throws IOException {
		if(!readString(data).equals(chunkID)) {
			int chunkSize = data.getInt();
			data.position(data.position() + chunkSize);
			if(!data.hasRemaining())
				throw new IOException("Could not find '" + chunkID + "' chunk");
			findChunk(chunkID, data);
		}
	}
	
	private static void readFormat(ByteBuffer data, WAVEDecoderNIO decoder) throws IOException {
		data.position(data.position() + 4); //skip subchunk1size for now
		
		short format = data.getShort();
		if(format != 1) //read the format, 1 for PCM
			throw new IOException("audio format is not PCM data, format: " + format);
		
		decoder.numChannels = data.getShort(); //read number of channels (1 or 2)
		decoder.sampleRate = data.getInt(); //read sample rate (ie 44100 44100 Hz)
		decoder.byteRate = data.getInt(); //read byte rate (SampleRate * NumChannels * BitsPerSample/8)
		decoder.blockAlign = data.getShort(); //read block align (NumChannels * BitsPerSample/8)
		decoder.bitsPerSample = data.getShort(); //bits per sample 8 for 8 bits, 16 for 16 bits, etc.
	}
	
	private static void readData(ByteBuffer in, WAVEDecoderNIO decoder) {
		//SubChunk2Size (NumSamples * NumChannels * BitsPerSample/8) aka the amount of space I need to store data
		//WAVE data is interleaved left/right sample, each sample is bits per sample large
		int size = in.getInt();
		in.limit(in.position() + size);
		decoder.data = BufferUtils.createByteBuffer(size).put(in).flip();
		in.limit(in.capacity());
	}

}
