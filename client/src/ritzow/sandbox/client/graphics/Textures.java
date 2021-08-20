package ritzow.sandbox.client.graphics;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.lwjgl.BufferUtils;
import ritzow.sandbox.client.data.StandardClientProperties;

public final class Textures {

	public static TextureAtlas buildAtlas(TextureData... textures) {
		return new TextureAtlas(textures);
	}

	public static TextureData loadTextureName(String name) throws IOException {
		return loadTextureData(StandardClientProperties.TEXTURES_PATH.resolve(name + ".png"));
	}

	public static TextureData loadTextureData(Path file) throws IOException {
		try(var in = Files.newInputStream(file)) {
			PNGDecoder decoder = new PNGDecoder(in);
			ByteBuffer pixels = BufferUtils.createByteBuffer(decoder.getWidth() * decoder.getHeight() * 4);
			decoder.decode(pixels, decoder.getWidth() * 4, Format.RGBA);
			byte[] data = new byte[pixels.flip().remaining()];
			pixels.get(data);
			return new TextureData(decoder.getWidth(), 4, data);
		}
	}
}
