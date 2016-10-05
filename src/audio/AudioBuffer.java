package audio;

import static org.lwjgl.openal.AL10.*;

import java.nio.ByteBuffer;

public final class AudioBuffer  {
    private int bufferID;
    
    public AudioBuffer() { 
    	bufferID = alGenBuffers();
    }
    
    public AudioBuffer(WaveformReader reader) {
    	this();
    	buffer(reader.getData(), reader.getNumChannels(), reader.getBitsPerSample(), reader.getSampleRate());
    }
    
    public void buffer(ByteBuffer data, short channels, int bitsPerSample, int frequency) {
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
    	
    	alBufferData(bufferID, format, data, frequency);
    }

    public void delete() {
    	alDeleteBuffers(bufferID);
    }
    
    public int getID() {
    	return bufferID;
    }
}