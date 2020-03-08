package ritzow.sandbox.client.audio;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public final class JavaxAudioSystem implements ritzow.sandbox.client.audio.AudioSystem {
	private final Collection<SourceDataLine> lines;
	private final DataLine.Info info;
	private final Mixer mixer;
	private final Map<Sound, byte[]> sounds;

	public static JavaxAudioSystem create(AudioData init) throws IOException {
		return new JavaxAudioSystem(init);
	}

	public JavaxAudioSystem(AudioData init) throws IOException {
		try {
			mixer = AudioSystem.getMixer(null); //get default mixer
			mixer.open();
			info = new DataLine.Info(SourceDataLine.class, getFormat(init));
			lines = new ConcurrentLinkedQueue<>();
			sounds = new ConcurrentHashMap<>();
			for(int i = 0; i < 5; i++) {
				createLine();
			}
		} catch (LineUnavailableException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void registerSound(Sound id, AudioData data) {
		if(sounds.containsKey(id))
			throw new IllegalStateException(id + " already registered");
		sounds.put(id, getData(data));
	}

	private static byte[] getData(AudioData info) {
		byte[] data = new byte[info.getData().capacity()];
		info.getData().get(data);
		return data;
	}

	private static AudioFormat getFormat(AudioData info) {
		return new AudioFormat(
			info.getSampleRate(),
			info.getBitsPerSample(),
			info.getChannels(),
			info.isSigned(),
			false
		);
	}

	private final ExecutorService worker = Executors.newCachedThreadPool();

	@Override
	public void playSound(Sound sound, float x, float y, float velocityX, float velocityY, float gain, float pitch) {
		worker.execute(()-> {
			byte[] data = sounds.get(sound);
			if(data == null)
				throw new IllegalArgumentException("no such sound");
			selectLine().write(data, 0, data.length);
		});
	}

	private SourceDataLine selectLine() {
		synchronized(lines) {
			for(var line : lines) {
				if(!line.isActive()) {
					return line;
				}
			}
		}

		if(!reachedMaxLines()) {
			try {
				var line = createLine();
				return line;
			} catch (LineUnavailableException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	private boolean reachedMaxLines() {
		int max = mixer.getMaxLines(info);
		return max != -1 && max > lines.size();
	}

	private SourceDataLine createLine() throws LineUnavailableException {
		var line = (SourceDataLine)mixer.getLine(info);
		line.open(); line.start();
		lines.add(line);
		return line;
	}

	@Override
	public void playSoundGlobal(Sound sound, float gain, float pitch) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public void setVolume(float gain) {
		synchronized(lines) {
			for(var line : lines) {
				((FloatControl)line.getControl(Type.MASTER_GAIN)).setValue(gain);
			}
		}
	}

	@Override
	public void setPosition(float x, float y) {

	}

	@Override
	public void close() {
		for(var line : lines) {
			line.close();
		}
		mixer.close();
		worker.shutdown();
	}

}
