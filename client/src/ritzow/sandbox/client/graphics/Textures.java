package ritzow.sandbox.client.graphics;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

public final class Textures {
	public static OpenGLTexture GRASS;
	public static OpenGLTexture BLUE_SQUARE;
	public static OpenGLTexture RED_SQUARE;
	public static OpenGLTexture GREEN_FACE;
	public static OpenGLTexture DIRT;
	public static OpenGLTexture ATLAS;
	
	public static void loadAll(File directory) throws IOException {
		Textures.BLUE_SQUARE = 	load(directory, "blueSquare.png");
		Textures.RED_SQUARE = 	load(directory, "redSquare.png");
		Textures.GREEN_FACE = 	load(directory, "greenFace.png");
		Textures.DIRT = 		load(directory, "dirt.png");
		Textures.GRASS = 		load(directory, "grass.png");
		//Textures.ATLAS = new TextureAtlas().toTexture();
	}
	
	private static OpenGLTexture load(File directory, String fileName) throws FileNotFoundException, IOException {
		return loadTexture(new FileInputStream(new File(directory, fileName)));
	}
	
	public static OpenGLTexture loadTexture(InputStream input) throws IOException {
		PNGDecoder decoder = new PNGDecoder(input);
		ByteBuffer pixels = BufferUtils.createByteBuffer(decoder.getWidth() * decoder.getHeight() * 4);
		decoder.decodeFlipped(pixels, decoder.getWidth() * 4, Format.RGBA);
		pixels.flip();
		input.close();
		return new OpenGLTexture(pixels, decoder.getWidth(), decoder.getHeight());
	}
}
