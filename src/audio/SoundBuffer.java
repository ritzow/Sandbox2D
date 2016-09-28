package audio;

import static org.lwjgl.openal.AL10.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import org.kc7bfi.jflac.*;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteData;
import org.lwjgl.BufferUtils;

public final class SoundBuffer implements PCMProcessor { //about audio format: http://stackoverflow.com/questions/3957025/what-does-a-audio-frame-contain
    private ArrayList<Short> sampleData;
	private ShortBuffer decodedData;
    private int bufferID;
    
    public SoundBuffer(String file) { 
    	bufferID = alGenBuffers();
    	loadData(file);
    }

    public void loadData(String file) {
    	try {
    		sampleData = new ArrayList<Short>();
    		FileInputStream fileStream = new FileInputStream(file);
    		FLACDecoder decoder = new FLACDecoder(fileStream);
    		decoder.addPCMProcessor(this);
    		decoder.decode();
    		fileStream.close();
    		decodedData = BufferUtils.createShortBuffer(sampleData.size());

    		for(int i = 0; i < sampleData.size(); i++) {
    			decodedData.put(sampleData.get(i));
    		}
    		
    		int bits = decoder.getStreamInfo().getBitsPerSample();
    		int channels = decoder.getStreamInfo().getChannels();
    		int format = 0;
    		
    		if(bits == 16 && channels == 2) 		format = AL_FORMAT_STEREO16;
    		else if(bits == 8 && channels == 2) 	format = AL_FORMAT_STEREO8;
    		else if(bits == 16 && channels == 1) 	format = AL_FORMAT_MONO16;
    		else if(bits == 8 && channels == 1) 	format = AL_FORMAT_MONO8;
    	
    		alBufferData(bufferID, format, decodedData, decoder.getStreamInfo().getSampleRate());

    		sampleData.clear(); sampleData = null;
    		decodedData.clear(); decodedData = null;
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }

    public void delete() {
    	alDeleteBuffers(bufferID);
    }
    
    public int getID() {
    	return bufferID;
    }

	@Override
	public void processStreamInfo(StreamInfo streamInfo) {
		System.out.println(streamInfo.getAudioFormat());
	}
	
	@Override
	public void processPCM(ByteData pcm) {
		for(int i = 0; i < pcm.getLen(); i+=2) {
			sampleData.add((short) (pcm.getData(i)<<8 | pcm.getData(i + 1) & 0xFF));
		}
	}
}