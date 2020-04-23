package ritzow.sandbox.client.audio;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

//TODO use http://www.softsynth.com/jsyn/ some day to create cool sound effects
public final class OpenALAudioSystem implements AudioSystem {
	private static long alContext;
	private static long device;
	private static int[] sources;
	private static boolean created;

	//TODO this takes 500 ms to load, mainly device initialization and context creation
	public static void initOpenAL() {
		ALC.create();
		//TODO enumerate available devices and pick one, then reuse the code if a device is disconnected
		device = alcOpenDevice((ByteBuffer)null);
		alContext = alcCreateContext(device, BufferUtils.createIntBuffer(2));

		ALCCapabilities alcCaps = ALC.createCapabilities(device);
		alcMakeContextCurrent(alContext);

		if(!AL.createCapabilities(alcCaps).OpenAL11) {
			alcMakeContextCurrent(0);
			alcDestroyContext(alContext);
			alcCloseDevice(device);
			ALC.destroy();
			throw new UnsupportedOperationException("OpenAL 1.1 not supported");
		}
		checkErrors();
		sources = new int[16];
		alGenSources(sources);
	}

	public static OpenALAudioSystem create() {
		if(created)
			throw new IllegalStateException("already created");
		created = true;
		return new OpenALAudioSystem();
	}

	public static Map<Integer, Integer> getAttributes() {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			var attributes = stack.mallocInt(1);
			alcGetIntegerv(device, ALC_ATTRIBUTES_SIZE, attributes);
			int attributesCount = attributes.get();
			attributes = stack.mallocInt(attributesCount * 2);
			alcGetIntegerv(device, ALC_ALL_ATTRIBUTES, attributes);
			Map<Integer, Integer> attributePairs = new HashMap<>(attributesCount);
			while(attributes.hasRemaining()) {
				attributePairs.put(attributes.get(), attributes.get());
			}

			return attributePairs;
		}
	}

	public static void shutdown() {
		alDeleteSources(sources);
		alcMakeContextCurrent(0);
		alcDestroyContext(alContext);
		alcCloseDevice(device);
		ALC.destroy();
	}

	public static void checkErrors() {
		int error = alGetError();
		if(error != AL_NO_ERROR) {
			throw new OpenALException("Last Error: " + error);
		}
	}

	private final Map<Sound, Integer> sounds;

	public OpenALAudioSystem() {
		sounds = new HashMap<>();
	}

	@Override
	public void close() {
		for(int buffer : sounds.values()) {
			alDeleteBuffers(buffer);
		}
		OpenALAudioSystem.shutdown();
	}

	@Override
	public void registerSound(Sound sound, AudioData data) {
		if(sounds.containsKey(sound))
			throw new UnsupportedOperationException("already a sound associated with sound " + sound);
		int format = switch(data.getBitsPerSample()) {
			case 8 -> switch(data.getChannels()) {
				case 1 -> AL_FORMAT_MONO8;
				case 2 -> AL_FORMAT_STEREO8;
				default -> throw new UnsupportedOperationException("invalid channel count");
			};
			case 16 -> switch(data.getChannels()) {
				case 1 -> AL_FORMAT_MONO16;
				case 2 -> AL_FORMAT_STEREO16;
				default -> throw new UnsupportedOperationException("invalid channel count");
			};
			default -> throw new UnsupportedOperationException("unsupported bits per sample");
		};

		int buffer = alGenBuffers();
		alBufferData(buffer, format, data.getData(), data.getSampleRate());
		sounds.put(sound, buffer);
		checkErrors();
	}

	@Override
	public void playSound(Sound sound, float x, float y, float velocityX, float velocityY, float gain, float pitch) {
		Integer bufferID = sounds.get(sound);
		if(bufferID == null)
			throw new IllegalStateException("soundID " + sound + " does not exist");
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
	public void playSoundGlobal(Sound sound, float gain, float pitch) {
		throw new UnsupportedOperationException("global playback not supported");
	}

	@Override
	public void setVolume(float gain) {
		alListenerf(AL_GAIN, gain);
	}

	@Override
	public void setPosition(float x, float y) {
		alListener3f(AL_POSITION, x, y, 0);
	}
}
