package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class TextureAtlas {
	private byte[] image;
	private int width, height;
	private final Map<TextureData, float[]> coordinates;
	
	public TextureAtlas(TextureData... textures) {
		coordinates = new HashMap<TextureData, float[]>();
		
	}
	
	public float[] getTextureCoordinates(TextureData texture) {
		return coordinates.get(texture);
	}
	
	public OpenGLTexture toTexture() {
		return new OpenGLTexture(ByteBuffer.allocateDirect(image.length).order(ByteOrder.nativeOrder()).put(image), width, height);
	}
	
	public byte[] getData() {
		return image;
	}
}
