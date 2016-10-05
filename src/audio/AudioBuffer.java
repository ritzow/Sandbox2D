package audio;

import static org.lwjgl.openal.AL10.*;

public final class AudioBuffer {
    private int bufferID;
    
    public AudioBuffer(String file) { 
    	bufferID = alGenBuffers();
    }

    public void delete() {
    	alDeleteBuffers(bufferID);
    }
    
    public int getID() {
    	return bufferID;
    }
}