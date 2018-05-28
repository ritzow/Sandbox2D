package ritzow.sandbox.client.graphics;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.lwjgl.BufferUtils;

public final class Textures {
	
	public static final float[] FULL_TEXTURE_COORDS = {
			0f, 1f,
			0f, 0f,
			1f, 0f,
			1f, 1f
	};
	
	public static TextureAtlas buildAtlas(TextureData... textures) {
		return new TextureAtlas(textures);
	}
	
	public static TextureData loadTextureName(String name) throws IOException {
		PNGDecoder decoder = new PNGDecoder(Files.newInputStream(Paths.get("resources/assets/textures", name + ".png")));
		ByteBuffer pixels = BufferUtils.createByteBuffer(decoder.getWidth() * decoder.getHeight() * 4);
		decoder.decodeFlipped(pixels, decoder.getWidth() * 4, Format.RGBA);
		pixels.flip();
		byte[] data = new byte[pixels.remaining()];
		pixels.get(data);
		return new TextureData(decoder.getWidth(), 4, data);
	}

	public static OpenGLTexture loadTexture(InputStream input) throws IOException {
		PNGDecoder decoder = new PNGDecoder(input);
		ByteBuffer pixels = BufferUtils.createByteBuffer(decoder.getWidth() * decoder.getHeight() * 4);
		decoder.decodeFlipped(pixels, decoder.getWidth() * 4, Format.RGBA);
		pixels.flip();
		return new OpenGLTexture(pixels, decoder.getWidth(), decoder.getHeight());
	}
}
