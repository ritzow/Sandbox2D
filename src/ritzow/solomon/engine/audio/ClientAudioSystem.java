package ritzow.solomon.engine.audio;

import static org.lwjgl.openal.AL10.AL_BUFFER;
import static org.lwjgl.openal.AL10.AL_GAIN;
import static org.lwjgl.openal.AL10.AL_INITIAL;
import static org.lwjgl.openal.AL10.AL_PITCH;
import static org.lwjgl.openal.AL10.AL_POSITION;
import static org.lwjgl.openal.AL10.AL_SOURCE_STATE;
import static org.lwjgl.openal.AL10.AL_STOPPED;
import static org.lwjgl.openal.AL10.AL_VELOCITY;
import static org.lwjgl.openal.AL10.alDeleteSources;
import static org.lwjgl.openal.AL10.alGenSources;
import static org.lwjgl.openal.AL10.alGetSourcei;
import static org.lwjgl.openal.AL10.alListener3f;
import static org.lwjgl.openal.AL10.alListenerf;
import static org.lwjgl.openal.AL10.alSource3f;
import static org.lwjgl.openal.AL10.alSourcePlay;
import static org.lwjgl.openal.AL10.alSourcef;
import static org.lwjgl.openal.AL10.alSourcei;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcGetInteger;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.openal.ALC11.ALC_ALL_DEVICES_SPECIFIER;
import static org.lwjgl.openal.ALC11.ALC_MONO_SOURCES;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.openal.ALUtil;

public class ClientAudioSystem implements AudioSystem {
	
	private static int[] sources;
	private static long context;
	private static long device;
	
	public ClientAudioSystem() {
		device = alcOpenDevice((ByteBuffer)null);
		context = alcCreateContext(device, (IntBuffer)null);
		ALCCapabilities alcCaps = ALC.createCapabilities(device);
		alcMakeContextCurrent(context);
		ALCapabilities alCaps = AL.createCapabilities(alcCaps);
		
		if(!alCaps.OpenAL11) {
			System.err.println("OpenAL 1.1 not supported");
			shutdown();
		}
		
		try {
			Sounds.loadAll(new File("resources/assets/audio"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sources = new int[alcGetInteger(device, ALC_MONO_SOURCES)];
		alGenSources(sources);
	}
	
	public void shutdown() {
		alDeleteSources(sources);
		Sounds.deleteAll();
		alcMakeContextCurrent(0);
		alcDestroyContext(context);
		alcCloseDevice(device);
		ALC.destroy();
	}

	@Override
	public void playSound(int sound, float x, float y, float velocityX, float velocityY, float gain, float pitch) {
		for(int i = 0; i < sources.length; i++) {
			int state = alGetSourcei(sources[i], AL_SOURCE_STATE);
			if(state == AL_STOPPED || state == AL_INITIAL) {
				alSourcei(sources[i], AL_BUFFER, sound); //TODO what is buffer id??
				alSource3f(sources[i], AL_POSITION, x, y, 0);
				alSource3f(sources[i], AL_VELOCITY, velocityX, velocityY, 0);
				alSourcef(sources[i], AL_GAIN, gain);
				alSourcef(sources[i], AL_PITCH, pitch);
				alSourcePlay(sources[i]);
			}
		}
	}

	@Override
	public void playSoundGlobal(int sound, float gain, float pitch) {
		System.out.println("client global sound playing not implemented");
	}

	public void setVolume(float gain) {
		alListenerf(AL_GAIN, gain);
	}

	public void setRelativePosition(float x, float y) {
		alListener3f(AL_POSITION, x, y, 0);
	}
	
	public void listDevices() {
		List<String> deviceList = ALUtil.getStringList(0, ALC_ALL_DEVICES_SPECIFIER);
		System.out.println("OpenAL Device List:");
		for(String device : deviceList) {
			System.out.println("	" + device);
		}
	}
	
}
