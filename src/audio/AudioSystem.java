package audio;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.ALC_ALL_DEVICES_SPECIFIER;
import static org.lwjgl.openal.ALC11.ALC_MONO_SOURCES;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import org.lwjgl.openal.*;
import resource.Sounds;

public class AudioSystem {
	
	private static int[] sources;
	private static long context;
	private static long device;
	
	public static void start() {
		
		device = alcOpenDevice((ByteBuffer)null);
		context = alcCreateContext(device, (IntBuffer)null);
		ALCCapabilities alcCaps = ALC.createCapabilities(device);
		alcMakeContextCurrent(context);
		ALCapabilities alCaps = AL.createCapabilities(alcCaps);
		
		if(!alCaps.OpenAL10) {
			System.err.println("OpenAL 1.0 not supported");
			stop();
		}
		
		try {
			Sounds.loadAll(new File("resources/assets/audio"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sources = new int[alcGetInteger(device, ALC_MONO_SOURCES)];
		alGenSources(sources);
	}
	
	public static void stop() {
		Sounds.deleteAll();
		alcMakeContextCurrent(0);
		alcDestroyContext(context);
		alcCloseDevice(device);
		ALC.destroy();
	}
	
	public static int playSound(int buffer, float x, float y, float velocityX, float velocityY, float gain, float pitch) {
		for(int i = 0; i < sources.length; i++) {
			int state = alGetSourcei(sources[i], AL_SOURCE_STATE);
			if(state == AL_STOPPED || state == AL_INITIAL) {
				alSourcei(sources[i], AL_BUFFER, buffer);
				alSource3f(sources[i], AL_POSITION, x, y, 0);
				alSource3f(sources[i], AL_VELOCITY, velocityX, velocityY, 0);
				alSourcef(sources[i], AL_GAIN, gain);
				alSourcef(sources[i], AL_PITCH, pitch);
				alSourcePlay(sources[i]);
				return sources[i];
			}
		}
		
		return -1;
	}
	
	public static void stopSound(int source) {
		alSourceStop(source);
	}
	
	public static void pauseSound(int source) {
		alSourcePause(source);
	}
	
	public static void rewindSound(int source) {
		alSourceRewind(source);
	}
	
	public static void close() {
		alDeleteSources(sources);
	}
	
	public static void setListenerPosition(float x, float y, float z) {
		alListener3f(AL_POSITION, x, y, z);
	}
	
	public static void setListenerVelocity(float velocityX, float velocityY) {
		alListener3f(AL_VELOCITY, velocityX, velocityY, 0);
	}
	
	public static void setListenerProperties(float x, float y, float z, float velocityX, float velocityY, float directionX, float directionY) {
		alListener3f(AL_POSITION, x, y, z);
		alListener3f(AL_VELOCITY, velocityX, velocityY, 0);
		alListenerfv(AL_ORIENTATION, new float[] {directionX, directionY, 0, 0, 1, 0});
	}
	
	public static void setVolume(float gain) {
		alListenerf(AL_GAIN, gain);
	}
	
	public static void printDevices() {
		List<String> deviceList = ALUtil.getStringList(0, ALC_ALL_DEVICES_SPECIFIER);
		System.out.println("OpenAL Device List:");
		for(String device : deviceList) {
			System.out.println("	" + device);
		}
	}
}