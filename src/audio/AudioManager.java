package audio;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.ALC_ALL_DEVICES_SPECIFIER;

import java.io.File;
import java.io.FileInputStream;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;
import main.Exitable;
import main.Installable;
import org.lwjgl.openal.*;

public class AudioManager implements Runnable, Installable, Exitable {
	private volatile boolean setupComplete;
	private volatile boolean exit;
	private volatile boolean finished;
	private volatile LinkedList<Sound> soundQueue;

	@Override
	public void run() {
		soundQueue = new LinkedList<Sound>();
		
		List<String> deviceList = ALUtil.getStringList(0, ALC_ALL_DEVICES_SPECIFIER);
		System.out.println("OpenAL Device List:");
		for(String device : deviceList) {
			System.out.println("	" + device);
		}
		long alDevice = alcOpenDevice(deviceList.get(0)); //TODO select first device
		System.out.println("Selected Device: " + alcGetString(alDevice, ALC_DEVICE_SPECIFIER));
		long alContext = alcCreateContext(alDevice, (IntBuffer)null);
		ALCCapabilities alcCaps = ALC.createCapabilities(alDevice);
		alcMakeContextCurrent(alContext);
		ALCapabilities alCaps = AL.createCapabilities(alcCaps);
		
		if(!alCaps.OpenAL10)
			try {
				throw new Exception("OpenAL 1.0 not supported!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		try {
			WaveformReader reader = new WaveformReader(new FileInputStream(new File("resources/assets/audio/monoTest.wav")));
			reader.decode(); System.out.println(reader);
			AudioBuffer buffer = new AudioBuffer(reader);
			Sound testSound = new Sound(buffer, 0, 0, 0, 0, 1, 1);
			testSound.setLooping(true);
			setListenerParameters(0, 0, 0, 0, 0, 0);
			testSound.play(); //TODO sound does not play, immediately "stops", which means it does start, but also doesnt play
			//System.out.println("Sound playing: " + testSound.isPlaying() + " (currently: " + testSound.getState() + ")");
			
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
