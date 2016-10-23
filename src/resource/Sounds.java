package resource;

import audio.AudioBuffer;
import audio.WAVEDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Sounds {
	public static AudioBuffer BLOCK_BREAK;
	
	public static void loadAll(String directory) throws IOException {
		
		if(!directory.endsWith("/"))
			directory += "/";
		
		WAVEDecoder reader = new WAVEDecoder(new FileInputStream(new File(directory + "bzzzt.wav")));
		reader.decode();
		Sounds.BLOCK_BREAK = new AudioBuffer(reader);
	}
}
