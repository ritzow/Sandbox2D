package ritzow.sandbox.server;

import ritzow.sandbox.audio.AudioSystem;

public class ServerAudioSystem implements AudioSystem {

	@Override
	public void playSound(int sound, float x, float y, float velocityX, float velocityY, float gain, float pitch) {
		System.out.println("server audio playsound not implemented");
	}

	@Override
	public void playSoundGlobal(int sound, float gain, float pitch) {
		System.out.println("server audio global playsound not implemented");
	}
}
