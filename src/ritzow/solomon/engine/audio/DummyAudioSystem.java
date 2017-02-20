package ritzow.solomon.engine.audio;

public class DummyAudioSystem implements AudioSystem {

	@Override
	public void playSound(int sound, float x, float y, float velocityX, float velocityY, float gain, float pitch) {
		System.out.println("played dummy sound");
	}

	@Override
	public void playSoundGlobal(int sound, float gain, float pitch) {
		System.out.println("played global dummy sound");
	}

}
