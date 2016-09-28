package main;

import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.AL10.*;

import audio.Sound;
import audio.SoundBuffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

public class AudioManager implements Runnable, Installable, Exitable {
	private volatile boolean setupComplete;
	private volatile boolean exit;
	private volatile boolean finished;
	private volatile LinkedList<Sound> soundQueue;

	@Override
	public void run() {
		alcMakeContextCurrent(alcCreateContext(alcOpenDevice((ByteBuffer)null), (IntBuffer)null));
		AL.createCapabilities(ALC.createCapabilities(alcGetContextsDevice(alcGetCurrentContext())));
		
		soundQueue = new LinkedList<Sound>();
		
		SoundBuffer buffer = new SoundBuffer("resources/assets/audio/bloopityBloop.flac");
		
		int testSource = alGenSources();
//		alListenerfv(AL_POSITION, new float[] {0f, 0f, 0f});
		alSourcei(testSource, AL_BUFFER, buffer.getID()); //makes the source a static source, rather than a streaming source (alSourceQueueBuffers)
//		alSourcefv(testSource, AL_POSITION, new float[] {0.0f, 0.0f, 0.0f});
//		alSourcef(testSource, AL_GAIN, 1.0f);
//		alSourcei(testSource, AL_LOOPING, AL_TRUE);
		System.out.println("Buffer Info: " + alGetBufferi(buffer.getID(), AL_CHANNELS));
		alSourcePlay(testSource);
		
		System.out.println("Source playing: " + (alGetSourcei(testSource, AL_SOURCE_STATE) == AL_PLAYING));
		
		int error; while((error = alGetError()) != AL_NO_ERROR) {System.out.println("OpenAL error: " + error);}
		
		synchronized(this) {
			setupComplete = true;
			notifyAll();
		}
		
		while(!exit) {
			while(!soundQueue.isEmpty()) {
				//play sound at index 0 using soundQueue.getFirst()
				System.out.println("played a sound");
				soundQueue.removeFirst();
			}
			
			synchronized(this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		int error2; while((error2 = alGetError()) != AL_NO_ERROR) {System.out.println("OpenAL error: " + error2);}
		
		long context = alcGetCurrentContext();
		long device = alcGetContextsDevice(context);
		
		alcMakeContextCurrent(0);
		alcDestroyContext(context);
		alcCloseDevice(device);
		ALC.destroy();
		
		synchronized(this) {
			finished = true;
			notifyAll();
		}
	}
	
	public void playSound(Sound sound) {
		soundQueue.add(sound);
		synchronized(this) {
			notifyAll();
		}
	}
	
	public void exit() {
		exit = true;
		
		synchronized(this) {
			notifyAll();
		}
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public boolean isSetupComplete() {
		return setupComplete;
	}
	
}
