package audio;

import static org.lwjgl.openal.AL10.*;

public final class Sound {
	
	private int sourceID;
	
	public Sound(AudioBuffer buffer, float x, float y, float velocityX, float velocityY, float gain, float pitch) {
		this.sourceID = alGenSources();
		alSourcei(sourceID, AL_BUFFER, buffer.getID());
		alSource3f(sourceID, AL_POSITION, x, y, 0);
		alSource3f(sourceID, AL_VELOCITY, velocityX, velocityY, 0);
		alSourcef(sourceID, AL_GAIN, gain);
		alSourcef(sourceID, AL_PITCH, pitch);
	}
	
	public void play() {
		alSourcePlay(sourceID);
	}
	
	public void stop() {
		alSourceStop(sourceID);
	}
	
	public void pause() {
		alSourcePause(sourceID);
	}
	
	public void delete() {
		alDeleteSources(sourceID);
	}
	
	public void setLooping(boolean loop) {
		alSourcei(sourceID, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
	}
	
	public int getState() {
		return alGetSourcei(sourceID, AL_SOURCE_STATE);
	}
	
	public boolean isPlaying() {
		return alGetSourcei(sourceID, AL_SOURCE_STATE) == AL_PLAYING;
	}
	
	public boolean isPaused() {
		return alGetSourcei(sourceID, AL_SOURCE_STATE) == AL_PAUSED;
	}
	
	public boolean isStopped() {
		return alGetSourcei(sourceID, AL_SOURCE_STATE) == AL_STOPPED;
	}
}
