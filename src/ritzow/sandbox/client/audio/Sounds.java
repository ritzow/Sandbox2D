package ritzow.sandbox.client.audio;

import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_MONO8;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO8;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alDeleteBuffers;
import static org.lwjgl.openal.AL10.alGenBuffers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class Sounds {
	public static int BLOCK_BREAK;
	public static int BLOCK_PLACE;
	public static int ITEM_PICKUP;
	public static int THROW;
	public static int SNAP;
	
	public static void loadAll(File directory) throws IOException {
		Sounds.BLOCK_BREAK = loadAudio(new File(directory.getPath(), "dig.wav"));
		Sounds.BLOCK_PLACE = loadAudio(new File(directory.getPath(), "place.wav"));
		Sounds.ITEM_PICKUP = loadAudio(new File(directory.getPath(), "pop.wav"));
		Sounds.THROW = loadAudio(new File(directory.getPath(), "throw.wav"));
		Sounds.SNAP = loadAudio(new File(directory.getPath(), "snap.wav"));
	}
	
	public static int loadAudio(File file) throws IOException {
		FileInputStream input = new FileInputStream(file);
		WAVEDecoder decoder = new WAVEDecoder(input);
		decoder.decode();
		input.close();
		
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
    	
    	int buffer = alGenBuffers();
    	alBufferData(buffer, format, decoder.getData(), decoder.getSampleRate());
    	return buffer;
	}
	
	public static void deleteAll() {
		alDeleteBuffers(BLOCK_BREAK);
		alDeleteBuffers(BLOCK_PLACE);
		alDeleteBuffers(ITEM_PICKUP);
		alDeleteBuffers(THROW);
	}
}
