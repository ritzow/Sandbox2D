package ritzow.sandbox.client.audio;

import java.nio.file.Path;

public interface Sound {

	enum StandardSound implements Sound {
		BLOCK_BREAK(Path.of("dig.wav")),
		BLOCK_PLACE(Path.of("place.wav")),
		POP(Path.of("pop.wav")),
		SNAP(Path.of("snap.wav")),
		THROW(Path.of("throw.wav"));

		private final Path fileName;

		StandardSound(Path fileName) {
			this.fileName = fileName;
		}

		public Path fileName() {
			return fileName;
		}
	}
}
