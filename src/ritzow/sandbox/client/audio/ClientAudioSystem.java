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
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import ritzow.sandbox.audio.AudioSystem;

public final class ClientAudioSystem implements AudioSystem {
	
	//ensures there's only one instance of ClientAudioSystem because there can only be one OpenAL context
	private static volatile boolean initialized;
	
	private final int[] sources;
	private final long context;
	private final long device;
	
	public ClientAudioSystem() {
		if(initialized) {
			throw new UnsupportedOperationException("OpenAL already initialized");
		} else {
			initialized = true;
			device = alcOpenDevice((ByteBuffer)null);
			context = alcCreateContext(device, (IntBuffer)null);
			ALCCapabilities alcCaps = ALC.createCapabilities(device);
			alcMakeContextCurrent(context);
			
			if(!AL.createCapabilities(alcCaps).OpenAL11) {
				System.err.println("OpenAL 1.1 not supported");
				shutdown();
			}
			
			sources = new int[alcGetInteger(device, ALC_MONO_SOURCES)];
			alGenSources(sources);
		}
	}
	
	public synchronized void shutdown() {
		alDeleteSources(sources);
		Sounds.deleteAll();
		alcMakeContextCurrent(0);
		alcDestroyContext(context);
		alcCloseDevice(device);
		ALC.destroy();
	}

	@Override
	public synchronized void playSound(int sound, float x, float y, float velocityX, float velocityY, float gain, float pitch) {
		for(int source : sources) {
			int state = alGetSourcei(source, AL_SOURCE_STATE);
			if(state == AL_STOPPED || state == AL_INITIAL) {
				alSourcei(source, AL_BUFFER, sound);
				alSource3f(source, AL_POSITION, x, y, 0);
				alSource3f(source, AL_VELOCITY, velocityX, velocityY, 0);
				alSourcef(source, AL_GAIN, gain);
				alSourcef(source, AL_PITCH, pitch);
				alSourcePlay(source);
				break;
			}
		}
	}

	@Override
	public synchronized void playSoundGlobal(int sound, float gain, float pitch) {
		System.out.println("client global sound playing not implemented");
	}

	@SuppressWarnings("static-method")
	public synchronized void setVolume(float gain) {
		alListenerf(AL_GAIN, gain);
	}

	@SuppressWarnings("static-method")
	public synchronized void setPosition(float x, float y) {
		alListener3f(AL_POSITION, x, y, 0);
	}
}
