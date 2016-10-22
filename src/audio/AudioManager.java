package audio;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.ALC_ALL_DEVICES_SPECIFIER;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;
import org.lwjgl.openal.*;
import util.Exitable;
import util.Installable;

public class AudioManager implements Runnable, Installable, Exitable {
	private volatile boolean setupComplete;
	private volatile boolean exit;
	private volatile boolean finished;
	private volatile LinkedList<Sound> soundQueue;

	@Override
	public void run() {
		soundQueue = new LinkedList<Sound>();
		
		long alDevice = alcOpenDevice((ByteBuffer)null);
		long alContext = alcCreateContext(alDevice, (IntBuffer)null);
		ALCCapabilities alcCaps = ALC.createCapabilities(alDevice);
		alcMakeContextCurrent(alContext);
		ALCapabilities alCaps = AL.createCapabilities(alcCaps);
		
		if(!alCaps.OpenAL10) {
			System.err.println("OpenAL 1.0 not supported");
			closeContext();
			exit = true;
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
		
		closeContext();

		synchronized(this) {
			finished = true;
			notifyAll();
		}
	}
	
	public void closeContext() {
		long context = alcGetCurrentContext();
		long device = alcGetContextsDevice(context);
		
		alcMakeContextCurrent(0);
		alcDestroyContext(context);
		alcCloseDevice(device);
		ALC.destroy();
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
	
	public void printDevices() {
		List<String> deviceList = ALUtil.getStringList(0, ALC_ALL_DEVICES_SPECIFIER);
		System.out.println("OpenAL Device List:");
		for(String device : deviceList) {
			System.out.println("	" + device);
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
