package resource;

import audio.AudioBuffer;
import audio.WAVEDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Sounds {
	public static AudioBuffer BLOCK_BREAK;
	
	public static void loadAll(File directory) throws IOException {
		Sounds.BLOCK_BREAK = loadAudio(new File(directory.getPath(), "bzzzt.wav"));
	}
	
	public static AudioBuffer loadAudio(File file) throws IOException {
		WAVEDecoder decoder = new WAVEDecoder(new FileInputStream(file));
		decoder.decode();
		return new AudioBuffer(decoder);
	}
}
