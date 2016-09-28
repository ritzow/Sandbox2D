package main;

import static org.lwjgl.openal.ALC10.*;

import audio.Sound;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import org.lwjgl.openal.ALCCapabilities;

public class AudioManager implements Runnable, Installable, Exitable {
	
	private volatile boolean setupComplete;
	private volatile boolean exit;
	private volatile boolean finished;
	
	private volatile ArrayList<Sound> soundQueue;

	@Override
	public void run() {
		soundQueue = new ArrayList<Sound>();
		
		long device = alcOpenDevice((ByteBuffer)null);
		ALCCapabilities deviceCaps = ALC.createCapabilities(device);
		long context = alcCreateContext(device, (IntBuffer)null);
		alcMakeContextCurrent(context);
		AL.createCapabilities(deviceCaps);
		
		synchronized(this) {
			setupComplete = true;
			notifyAll();
		}
		
		while(!exit) {
			while(!soundQueue.isEmpty()) {
				//TODO play sound at index 0
				soundQueue.remove(0);
			}
			
			synchronized(this) {
				try {
					while(!exit && soundQueue.isEmpty()) {
						this.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
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
