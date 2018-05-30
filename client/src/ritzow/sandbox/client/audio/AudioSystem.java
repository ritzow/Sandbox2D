package ritzow.sandbox.client.audio;

public interface AudioSystem {
	void playSound(int sound, float x, float y, float velocityX, float velocityY, float gain, float pitch);
	void playSoundGlobal(int sound, float gain, float pitch);
	void registerSound(int id, SoundInfo data);
	void setVolume(float gain);
	void setPosition(float x, float y);
	void close();
}
