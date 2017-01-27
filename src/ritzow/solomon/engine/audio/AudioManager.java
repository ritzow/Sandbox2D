package ritzow.solomon.engine.audio;

public interface AudioManager {
	public void playSound(int sound, float x, float y, float velocityX, float velocityY, float gain, float pitch);
	public void playSoundGlobal(int sound);
}
