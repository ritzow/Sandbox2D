package ritzow.sandbox.client.graphics;

import java.util.HashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;

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
		return new OpenGLTexture(BufferUtils.createByteBuffer(image.length).put(image), width, height);
	}
	
	public byte[] getData() {
		return image;
	}

	@Override
	public String toString() {
		return "TextureAtlas [image=" + image.length + " bytes, width=" + width + ", height=" + height
				+ ", coordinates=" + coordinates + "]";
	}
	
	
}
