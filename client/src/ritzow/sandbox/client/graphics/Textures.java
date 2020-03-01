package ritzow.sandbox.client.graphics;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import org.lwjgl.BufferUtils;
import ritzow.sandbox.client.data.StandardClientProperties;

public final class Textures {

	public static TextureAtlas buildAtlas(TextureData... textures) {
		return new TextureAtlas(textures);
	}

	public static TextureData[] loadTextureNames(String... names) throws IOException {
		TextureData[] textures = new TextureData[names.length];
		for(int i = 0; i < textures.length; i++) {
			textures[i] = loadTextureName(names[i]);
		}
		return textures;
	}

	public static TextureData loadTextureName(String name) throws IOException {
		try(var in = Files.newInputStream(StandardClientProperties.TEXTURES_PATH.resolve(name + ".png"))) {
			PNGDecoder decoder = new PNGDecoder(in);
			ByteBuffer pixels = BufferUtils.createByteBuffer(decoder.getWidth() * decoder.getHeight() * 4);
			decoder.decode(pixels, decoder.getWidth() * 4, Format.RGBA);
			pixels.flip();
			byte[] data = new byte[pixels.remaining()];
			pixels.get(data);
			return new TextureData(decoder.getWidth(), 4, data);
		}
	}

	public static OpenGLTexture loadTexture(InputStream input) throws IOException {
		PNGDecoder decoder = new PNGDecoder(input);
		ByteBuffer pixels = BufferUtils.createByteBuffer(decoder.getWidth() * decoder.getHeight() * 4);
		decoder.decode(pixels, decoder.getWidth() * 4, Format.RGBA);
		pixels.flip();
		return new OpenGLTexture(pixels, decoder.getWidth(), decoder.getHeight());
	}
}
