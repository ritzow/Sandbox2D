package ritzow.sandbox.client.audio;

import java.io.IOException;
import ritzow.sandbox.client.audio.Sound.StandardSound;

import static ritzow.sandbox.client.data.StandardClientProperties.AUDIO_PATH;
import static ritzow.sandbox.client.util.ClientUtility.log;

//TODO audio system should know about camera location and other things
public interface AudioSystem {

	default void playSound(Sound sound, float x, float y, float velocityX, float velocityY) {
		playSound(sound, x, y, velocityX, velocityY, 1.0f, 1.0f);
	}

	void playSound(Sound sound, float x, float y, float velocityX, float velocityY, float gain, float pitch);
	void playSoundGlobal(Sound sound, float gain, float pitch);
	void registerSound(Sound sound, AudioData data);
	void setVolume(float gain);
	void setPosition(float x, float y);
	void close();

	static AudioSystem getDefault() {
		return Default.DEFAULT;
	}

	final class Default {
		private Default() {}
		private static final AudioSystem DEFAULT = loadDefault();
		private static AudioSystem loadDefault() {
			try {
				log().info("Loading audio system");
				OpenALAudioSystem.initOpenAL();
				AudioSystem audio = OpenALAudioSystem.create();
				audio.setVolume(1.0f);
				for(var sound : StandardSound.values()) {
					audio.registerSound(sound,
							WAVEDecoder.decode(AUDIO_PATH.resolve(sound.fileName())));
				}
				return audio;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
