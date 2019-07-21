package ritzow.sandbox.client.audio;

public class SilentAudioSystem implements AudioSystem {
	
	public static final SilentAudioSystem INSTANCE = new SilentAudioSystem();
	
	private SilentAudioSystem() {
		
	}

	@Override
	public void playSound(int sound, float x, float y, float velocityX, float velocityY, float gain, float pitch) {
		
	}

	@Override
	public void playSoundGlobal(int sound, float gain, float pitch) {
		
	}

	@Override
	public void registerSound(int id, SoundInfo data) {
		
	}

	@Override
	public void setVolume(float gain) {
		
	}

	@Override
	public void setPosition(float x, float y) {
		
	}

	@Override
	public void close() {
		
	}

}
