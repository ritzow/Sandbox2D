package ritzow.sandbox.client.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class WAVEDecoder implements AudioData {
	private short format; //1 for PCM audio, other numbers indicate compression
	private short numChannels; //1 = mono, 2 = stereo
	private int sampleRate; //8000, 44100
	private int byteRate; //sampleRate * numChannels * bitsPerSample/8
	private short blockAlign; //numChannels * bitsPerSample/8
	private short bitsPerSample; //8 bits = 8, 16 bits = 16, etc.
	private ByteBuffer data;
	
	private static final int HEADER_SIZE = 12, CHUNK_HEADER_SIZE = 8;
	private static final int FORMAT_PCM = 1;
	private static final String RIFF_HEADER = "RIFF", WAVE_HEADER = "WAVE";
	
	private WAVEDecoder() {
		
	}

	public static AudioData decode(Path file) throws IOException {
		try(SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
			//Read RIFF descriptor
			ByteBuffer headerBuffer = ByteBuffer.wrap(new byte[HEADER_SIZE]);
			channel.read(headerBuffer);
			if(!readTypeString(headerBuffer.array(), 0).equals(RIFF_HEADER))
				throw new IOException("file does not begin with the RIFF header");
			//skip ChunkSize of RIFF header, read format (check that it's WAVE)
			if(!readTypeString(headerBuffer.array(), 8).equals(WAVE_HEADER))
				throw new IOException("file format is not WAVE");
			
			WAVEDecoder decoder = new WAVEDecoder();
			ByteBuffer chunkHeader = ByteBuffer.wrap(new byte[CHUNK_HEADER_SIZE])
					.order(ByteOrder.LITTLE_ENDIAN);
			while(channel.position() < channel.size() - CHUNK_HEADER_SIZE) {
				channel.read(chunkHeader);
				String chunkType = readTypeString(chunkHeader.array(), 0);
				int chunkSize = chunkHeader.getInt(4);
				chunkHeader.clear();
				switch(chunkType) {
					case "fmt " -> readFormat(channel, decoder);
					case "data" -> {
						//SubChunk2Size (NumSamples * NumChannels * BitsPerSample/8) 
						//aka the amount of space I need to store data
						//WAVE data is interleaved left/right sample, each 
						//sample is bits per sample large
						decoder.data = ByteBuffer.allocateDirect(chunkSize).order(ByteOrder.nativeOrder());
						channel.read(decoder.data);
						decoder.data.flip();
					}
					default -> channel.position(channel.position() + chunkSize); //skip unknown
				}
			}
			return decoder;
		}
	}
	
	private static String readTypeString(byte[] buffer, int position) {
		return new String(buffer, position, 4, StandardCharsets.US_ASCII);
	}

	private static void readFormat(SeekableByteChannel channel, WAVEDecoder decoder) throws IOException {
		ByteBuffer data = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
		channel.read(data);
		decoder.format = data.flip().getShort();
		if(decoder.format != FORMAT_PCM) //read the format, 1 for PCM
			throw new UnsupportedOperationException("audio format is not PCM data, format: " + decoder.format);
		decoder.numChannels = data.getShort(); //read number of channels (1 or 2)
		decoder.sampleRate = data.getInt(); //read sample rate (ie 44100 44100 Hz)
		decoder.byteRate = data.getInt(); //read byte rate (SampleRate * NumChannels * BitsPerSample/8)
		decoder.blockAlign = data.getShort(); //read block align (NumChannels * BitsPerSample/8)
		decoder.bitsPerSample = data.getShort(); //bits per sample 8 for 8 bits, 16 for 16 bits, etc.
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
}
