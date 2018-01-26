package ritzow.sandbox.client.audio;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcGetInteger;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.openal.ALC11.ALC_MONO_SOURCES;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

public final class ClientAudioSystem implements AudioSystem {
	private static long alContext;
	private static long device;
	private static int[] sources;
	private static ClientAudioSystem audio;
	
	public static ClientAudioSystem getAudioSystem() {
		return audio == null ? audio = initialize() : audio;
	}
	
	private static ClientAudioSystem initialize() {
		device = alcOpenDevice((ByteBuffer)null);
		alContext = alcCreateContext(device, (IntBuffer)null);
		ALCCapabilities alcCaps = ALC.createCapabilities(device);
		alcMakeContextCurrent(alContext);
		
		if(!AL.createCapabilities(alcCaps).OpenAL11) {
			System.err.println("OpenAL 1.1 not supported");
			shutdown();
			return null;
		} else {
			sources = new int[alcGetInteger(device, ALC_MONO_SOURCES)];
			alGenSources(sources);
			return new ClientAudioSystem();
		}
	}
	
	public static void shutdown() {
		alDeleteSources(sources);
		alcMakeContextCurrent(0);
		alcDestroyContext(alContext);
		alcCloseDevice(device);
		ALC.destroy();
	}
	
	private final Map<Integer, Integer> sounds;
	
	public ClientAudioSystem() {
		sounds = new HashMap<>();
	}
	
	public void close() {
		for(int buffer : sounds.values()) {
			alDeleteBuffers(buffer);
		}
	}
	
	public void registerSound(int id, SoundInfo data) {
		Objects.requireNonNull(data);
    	int format = AL_FORMAT_STEREO16;
    	
    	if(data.getBitsPerSample() == 8) {
    		if(data.getChannels() == 1) {
    			format = AL_FORMAT_MONO8;
    		} else if(data.getChannels() == 2) {
    			format = AL_FORMAT_STEREO8;
    		} else {
    			throw new UnsupportedOperationException("invalid channel count");
    		}
    	} else if(data.getBitsPerSample() == 16) {
    		if(data.getChannels() == 1) {
    			format = AL_FORMAT_MONO16;
    		} else if(data.getChannels() == 2) {
    			format = AL_FORMAT_STEREO16;
    		} else {
    			throw new UnsupportedOperationException("invalid channel count");
    		}
    	} 
    	
    	int buffer = alGenBuffers();
    	alBufferData(buffer, format, data.getData(), data.getSampleRate());
    	if(sounds.containsKey(id))
    		throw new UnsupportedOperationException("already a sound associated with id " + id);
		sounds.put(id, buffer);
	}

	@Override
	public void playSound(int soundID, float x, float y, float velocityX, float velocityY, float gain, float pitch) {
		Integer bufferID = sounds.get(Integer.valueOf(soundID));
		if(bufferID == null)
			throw new IllegalStateException("soundID " + soundID + " does not exist");
		for(int source : sources) {
			int state = alGetSourcei(source, AL_SOURCE_STATE);
			if(state == AL_STOPPED || state == AL_INITIAL) {
				alSourcei(source, AL_BUFFER, bufferID);
				alSource3f(source, AL_POSITION, x, y, 0);
				alSource3f(source, AL_VELOCITY, velocityX, velocityY, 0);
				alSourcef(source, AL_GAIN, gain);
				alSourcef(source, AL_PITCH, pitch);
				alSourcePlay(source);
				return;
			}
		}
	}

	@Override
	public void playSoundGlobal(int sound, float gain, float pitch) {
		throw new UnsupportedOperationException("global playback not supported");
	}

	@SuppressWarnings("static-method")
	public void setVolume(float gain) {
		alListenerf(AL_GAIN, gain);
	}

	@SuppressWarnings("static-method")
	public void setPosition(float x, float y) {
		alListener3f(AL_POSITION, x, y, 0);
	}
}
