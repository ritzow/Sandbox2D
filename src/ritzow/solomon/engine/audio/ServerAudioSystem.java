package ritzow.solomon.engine.audio;

public class ServerAudioSystem implements AudioSystem {

	@Override
	public void playSound(int sound, float x, float y, float velocityX, float velocityY, float gain, float pitch) {
		//TODO send audio messages to clients
	}

	@Override
	public void playSoundGlobal(int sound, float gain, float pitch) {
		
	}
}
