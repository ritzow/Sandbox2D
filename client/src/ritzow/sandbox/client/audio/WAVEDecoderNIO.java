package ritzow.sandbox.client.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.lwjgl.BufferUtils;
import ritzow.sandbox.client.util.ClientUtility;

public class WAVEDecoderNIO implements AudioData {
	private short format; //1 for PCM audio, other numbers indicate compression
	private short numChannels; //1 = mono, 2 = stereo
	private int sampleRate; //8000, 44100
	private int byteRate; //sampleRate * numChannels * bitsPerSample/8
	private short blockAlign; //numChannels * bitsPerSample/8
	private short bitsPerSample; //8 bits = 8, 16 bits = 16, etc.
	private ByteBuffer data;
	
	private static final int FORMAT_PCM = 1;

	public static AudioData decode(Path file) throws IOException {
		ByteBuffer buffer = ClientUtility.load(file).order(ByteOrder.LITTLE_ENDIAN);
		checkHeader(buffer);
		return readChunks(buffer);
	}
	
	private WAVEDecoderNIO() {}
	
	@Override
	public String toString() {
		return new StringBuilder()
			.append("format=")
			.append(format)
			.append(" channels=")
			.append(numChannels)
			.append(" sample rate=")
			.append(sampleRate)
			.append(" byte rate=")
			.append(byteRate)
			.append(" block align=")
			.append(blockAlign)
			.append(" bits per sample=")
			.append(bitsPerSample)
			.toString();
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
		return data;
	}
	
	private static void checkHeader(ByteBuffer data) throws IOException {
		if(!readTypeString(data).equals("RIFF"))
			throw new IOException("file does not begin with the RIFF header");
		data.position(data.position() + 4); //skip ChunkSize for RIFF header
		if(!readTypeString(data).equals("WAVE"))
			throw new IOException("file format is not WAVE");
	}
	
	private static String readTypeString(ByteBuffer data) {
		byte[] header = new byte[4];
		data.get(header);
		return new String(header, StandardCharsets.US_ASCII);
	}

	private static WAVEDecoderNIO readChunks(ByteBuffer data) {
		var decoder = new WAVEDecoderNIO();
		while(data.hasRemaining()) {
			String chunkType = readTypeString(data);
			int chunkSize = data.getInt();
			switch(chunkType) {
				case "fmt " -> readFormat(data, decoder);
				case "data" -> readData(data, decoder, chunkSize);
				default -> data.position(data.position() + chunkSize);
			}
		}
		return decoder;
	}
	
	private static void readFormat(ByteBuffer data, WAVEDecoderNIO decoder) {
		decoder.format = data.getShort();
		if(decoder.format != FORMAT_PCM) //read the format, 1 for PCM
			throw new UnsupportedOperationException("audio format is not PCM data, format: " + decoder.format);
		decoder.numChannels = data.getShort(); //read number of channels (1 or 2)
		decoder.sampleRate = data.getInt(); //read sample rate (ie 44100 44100 Hz)
		decoder.byteRate = data.getInt(); //read byte rate (SampleRate * NumChannels * BitsPerSample/8)
		decoder.blockAlign = data.getShort(); //read block align (NumChannels * BitsPerSample/8)
		decoder.bitsPerSample = data.getShort(); //bits per sample 8 for 8 bits, 16 for 16 bits, etc.
	}
	
	private static void readData(ByteBuffer in, WAVEDecoderNIO decoder, int chunkSize) {
		//SubChunk2Size (NumSamples * NumChannels * BitsPerSample/8) aka the amount of space I need to store data
		//WAVE data is interleaved left/right sample, each sample is bits per sample large
		in.limit(in.position() + chunkSize);
		decoder.data = BufferUtils.createByteBuffer(chunkSize).put(in).flip();
		in.limit(in.capacity());
	}
}
