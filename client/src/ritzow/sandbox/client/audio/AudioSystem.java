package ritzow.sandbox.client.audio;

public interface AudioSystem {
	public void playSound(int sound, float x, float y, float velocityX, float velocityY, float gain, float pitch);
	public void playSoundGlobal(int sound, float gain, float pitch);
}
