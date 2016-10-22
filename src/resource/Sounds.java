package resource;

import audio.AudioBuffer;
import audio.WaveformReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Sounds {
	public static AudioBuffer BLOCK_BREAK;
	
	public static void loadAll(String directory) throws IOException {
		
		if(!directory.endsWith("/"))
			directory += "/";
		
		WaveformReader reader = new WaveformReader(new FileInputStream(new File(directory + "blockBreak.wav")));
		reader.decode();
		Sounds.BLOCK_BREAK = new AudioBuffer(reader);
	}
}
