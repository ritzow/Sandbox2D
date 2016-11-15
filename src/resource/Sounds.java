package resource;

import static org.lwjgl.openal.AL10.*;

import audio.WAVEDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class Sounds {
	public static int BLOCK_BREAK;
	public static int BLOCK_PLACE;
	public static int ITEM_PICKUP;
	public static int THROW;
	
	public static void loadAll(File directory) throws IOException {
		Sounds.BLOCK_BREAK = loadAudio(new File(directory.getPath(), "dig.wav"));
		Sounds.BLOCK_PLACE = loadAudio(new File(directory.getPath(), "place.wav"));
		Sounds.ITEM_PICKUP = loadAudio(new File(directory.getPath(), "pop.wav"));
		Sounds.THROW = loadAudio(new File(directory.getPath(), "throw.wav"));
	}
	
	public static int loadAudio(File file) throws IOException {
		WAVEDecoder decoder = new WAVEDecoder(new FileInputStream(file));
		decoder.decode();
		
    	int format = AL_FORMAT_STEREO16;
    	
    	if(decoder.getBitsPerSample() == 8) {
    		if(decoder.getChannels() == 1) {
    			format = AL_FORMAT_MONO8;
    		} else if(decoder.getChannels() == 2) {
    			format = AL_FORMAT_STEREO8;
    		}
    	} else if(decoder.getBitsPerSample() == 16) {
    		if(decoder.getChannels() == 1) {
    			format = AL_FORMAT_MONO16;
    		} else if(decoder.getChannels() == 2) {
    			format = AL_FORMAT_STEREO16;
    		}
    	}
    	
    	decoder.getData().position(0);
    	
    	int buffer = alGenBuffers();
    	alBufferData(buffer, format, decoder.getData(), decoder.getSampleRate());
    	
    	decoder.close();
    	
    	return buffer;
	}
	
	public static void deleteAll() {
		alDeleteBuffers(Sounds.BLOCK_BREAK);
		alDeleteBuffers(Sounds.BLOCK_PLACE);
		alDeleteBuffers(Sounds.ITEM_PICKUP);
		alDeleteBuffers(Sounds.THROW);
	}
}
