package ritzow.sandbox.client.audio;

import static ritzow.sandbox.client.data.StandardClientProperties.AUDIO_PATH;

import java.util.List;
import java.util.Map;

public interface AudioSystem {
	default void playSound(int sound, float x, float y, float velocityX, float velocityY) {
		playSound(sound, x, y, velocityX, velocityY, 1.0f, 1.0f);
	}
	
	void playSound(int sound, float x, float y, float velocityX, float velocityY, float gain, float pitch);
	void playSoundGlobal(int sound, float gain, float pitch);
	void registerSound(int id, AudioData data);
	void setVolume(float gain);
	void setPosition(float x, float y);
	void close();
	
	public static AudioSystem getDefault() {
		return Default.DEFAULT;
	}
	
	static final class Default {
		private static final AudioSystem DEFAULT = loadDefault();
		
		private static AudioSystem loadDefault() {
			try {
				OpenALAudioSystem.initialize();
				AudioSystem audio = OpenALAudioSystem.create();
				audio.setVolume(1.0f);

				var sounds = List.of(
					Map.entry(Sound.BLOCK_BREAK, "dig.wav"),
					Map.entry(Sound.BLOCK_PLACE, "place.wav"),
					Map.entry(Sound.POP, "pop.wav"),
					Map.entry(Sound.THROW, "throw.wav"),
					Map.entry(Sound.SNAP, "snap.wav")
				);

				for(var entry : sounds) {
					audio.registerSound(entry.getKey().code(), 
							WAVEDecoderNIO.decode(AUDIO_PATH.resolve(entry.getValue())));
				}
				return audio;
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
