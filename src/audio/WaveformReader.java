package audio;

import java.io.IOException;
import java.io.InputStream;

public final class WaveformReader {
	
	private InputStream input;
	private int dataSizeBytes;
	
	public WaveformReader(InputStream input) {
		this.input = input;
	}
	
	public void decode() throws IOException {
		checkHeader();
		readFormat();
	}
	
	private String readChunkID() throws IOException {
		byte[] chunkID = new byte[4]; input.read(chunkID, 0, 4);
		return new String(chunkID);
	}
	
	private void checkHeader() throws IOException {
		if(!readChunkID().equals("RIFF"))
			throw new IOException("file does not begin with the RIFF header");
		
		byte[] chunkSize = new byte[4]; input.read(chunkSize, 0, 4);
		this.dataSizeBytes = toInteger(flip(chunkSize));
		
		byte[] format = new byte[4]; input.read(format, 0, 4);
		if(!(new String(format).equals("WAVE")))
			throw new IOException("file format is not WAVE");
	} 
	
	private void readFormat() throws IOException {
		if(!readChunkID().equals("fmt "))
			throw new IOException("chunk id not fmt ");
		
	}
	
	private static int toInteger(byte[] data) {
		return  (((data[0] & 0xff) << 24) | ((data[1] & 0xff) << 16) | ((data[2] & 0xff) << 8) | (data[3] & 0xff));
	}
	
	private static byte[] flip(byte[] data) {
		byte temp;
		for(int i = 0; i < data.length/2; i++) {
			temp = data[i];
			data[i] = data[data.length - i - 1];
			data[data.length - i - 1] = temp;
		}
		return data;
	}
}
