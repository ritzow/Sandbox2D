package ritzow.sandbox.client.audio;

public class DefaultAudioSystem {
	private static AudioSystem audio;
	
	public static void setDefault(AudioSystem audio) {
		DefaultAudioSystem.audio = audio;
	}
	
	public static AudioSystem getDefault() {
		return audio;
	}
}
