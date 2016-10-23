package audio;

import static org.lwjgl.openal.AL10.*;
import java.nio.ByteBuffer;

public final class AudioBuffer  {
    private int bufferID;
    
    private ByteBuffer data;
    private int channels;
    private int bitsPerSample;
    private int frequency;
    
    public AudioBuffer(WAVEDecoder decoder) {
    	this(decoder.getData(), decoder.getChannels(), decoder.getBitsPerSample(), decoder.getSampleRate());
    }
    
    public AudioBuffer(ByteBuffer data, short channels, int bitsPerSample, int frequency) {
    	this.bufferID = alGenBuffers();
    	this.data = data;
    	this.channels = channels;
    	this.bitsPerSample = bitsPerSample;
    	this.frequency = frequency;
    	
    	buffer();
    }
    
    public void buffer() {
    	int format = AL_FORMAT_STEREO16;
    	
    	if(bitsPerSample == 8) {
    		if(channels == 1) {
    			format = AL_FORMAT_MONO8;
    		} else if(channels == 2) {
    			format = AL_FORMAT_STEREO8;
    		}
    	} else if(bitsPerSample == 16) {
    		if(channels == 1) {
    			format = AL_FORMAT_MONO16;
    		} else if(channels == 2) {
    			format = AL_FORMAT_STEREO16;
    		}
    	}
    	
    	data.position(0);
    	alBufferData(bufferID, format, data, frequency);
    }

    public void delete() {
    	alDeleteBuffers(bufferID);
    }
    
    public int getID() {
    	return bufferID;
    }
}