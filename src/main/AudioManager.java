package main;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

import audio.AudioBuffer;
import audio.Sound;
import audio.WaveformReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.io.File;
import java.io.FileInputStream;
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
		System.out.println(alcGetString(alcGetContextsDevice(alcGetCurrentContext()), ALC_DEVICE_SPECIFIER));
		soundQueue = new LinkedList<Sound>();
		
		try {
			WaveformReader reader = new WaveformReader(new FileInputStream(new File("resources/assets/audio/monoTest.wav")));
			reader.decode(); System.out.println(reader);
			AudioBuffer buffer = new AudioBuffer(reader);
			Sound testSound = new Sound(buffer, 0, 0, 0, 0, 1, 1);
			setListenerParameters(0, 0, 0, 0, 0, 0);
			testSound.play(); //TODO sound does not play, immediately "stops", which means it does start, but also doesnt play
			System.out.println("Sound playing: " + testSound.isPlaying() + " (currently: " + testSound.getState() + ")");
			
			int error;
			while((error = alGetError()) != AL_NO_ERROR) {
				System.out.println("OpenAL error " + error);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		synchronized(this) {
			setupComplete = true;
			notifyAll();
		}
		
		while(!exit) {
			while(!soundQueue.isEmpty()) {
				soundQueue.getFirst().play();
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
	
	public void setListenerParameters(float x, float y, float velocityX, float velocityY, float directionX, float directionY) {
		alListener3f(AL_POSITION, x, y, 0);
		alListener3f(AL_VELOCITY, velocityX, velocityY, 0);
		alListenerfv(AL_ORIENTATION, new float[] {directionX, directionY, 0, 0, 1, 0});
	}
	
	public void setVolume(float gain) {
		alListenerf(AL_GAIN, gain);
	}
	
	public void playSound(Sound sound) {
		soundQueue.add(sound);
		synchronized(this) {
			notifyAll();
		}
	}
	
	public void stopSound(Sound sound) {
		soundQueue.remove(sound);
		sound.stop();
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
