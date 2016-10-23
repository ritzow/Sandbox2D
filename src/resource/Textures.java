package resource;

import graphics.data.Texture;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import de.matthiasmann.twl.utils.*;
import de.matthiasmann.twl.utils.PNGDecoder.Format;

public class Textures {
	public static Texture GRASS_BLOCK;
	public static Texture BLUE_SQUARE;
	public static Texture RED_SQUARE;
	public static Texture GREEN_FACE;
	public static Texture CLOUDS;
	public static Texture DIRT;
	
	public static void loadAll(File directory) throws IOException {
		
		File[] files = directory.listFiles();
		
		for(File file : files) {
			if(file.getName().equals("blueSquare.png")) {
				Textures.BLUE_SQUARE = loadTexture(file);
			} else if(file.getName().equals("redSquare.png")) {
				Textures.RED_SQUARE = loadTexture(file);
			} else if(file.getName().equals("grass.png")) {
				Textures.GRASS_BLOCK = loadTexture(file);
			} else if(file.getName().equals("dirt.png")) {
				Textures.DIRT = loadTexture(file);
			} else if(file.getName().equals("clouds.png")) {
				Textures.CLOUDS = loadTexture(file);
			} else if(file.getName().equals("greenFace.png")) {
				Textures.GREEN_FACE = loadTexture(file);
			}
		}
	}
	
	public static Texture loadTexture(File file) throws IOException {
		FileInputStream data = new FileInputStream(file);
		PNGDecoder decoder = new PNGDecoder(data);
		ByteBuffer pixels = BufferUtils.createByteBuffer(decoder.getWidth() * decoder.getHeight() * 4);
		decoder.decodeFlipped(pixels, decoder.getWidth() * 4, Format.RGBA);
		pixels.flip();
		data.close();
		return new Texture(pixels, decoder.getWidth(), decoder.getHeight());
	}
}
