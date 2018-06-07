package ritzow.sandbox.client.audio;

public interface AudioSystem {
	default void playSound(int sound, float x, float y, float velocityX, float velocityY) {
		playSound(sound, x, y, velocityX, velocityY, 1.0f, 1.0f);
	}
	
	void playSound(int sound, float x, float y, float velocityX, float velocityY, float gain, float pitch);
	void playSoundGlobal(int sound, float gain, float pitch);
	void registerSound(int id, SoundInfo data);
	void setVolume(float gain);
	void setPosition(float x, float y);
	void close();
}
