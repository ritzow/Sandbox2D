package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;

public class TextureAtlas {
	private ByteBuffer texture;
	private int size;
	private final Map<TextureData, float[]> coordinates;
	
	TextureAtlas(TextureData... textures) {
		coordinates = new HashMap<TextureData, float[]>();
		texture = generateAtlas(textures, coordinates);
	}
	
	private ByteBuffer generateAtlas(TextureData[] textures, Map<TextureData, float[]> relations) {
		//TODO for now just put them all in a horizontal line
		int bytesPerPixel = textures[0].getBytesPerPixel();
		int widthPixels = calculateAtlasWidth(textures, bytesPerPixel);
		
		//create the atlas buffer
		ByteBuffer atlas = BufferUtils.createByteBuffer(widthPixels * widthPixels * bytesPerPixel);
		
		int pixelX = 0;
		for(TextureData tex : textures) {
			place(tex, atlas, 0, pixelX * bytesPerPixel, widthPixels * bytesPerPixel, bytesPerPixel);
			float leftX = pixelX/(float)widthPixels;
			float rightX = (pixelX + tex.getWidth())/(float)widthPixels;
			float bottomY =  tex.getHeight()/(float)widthPixels;
			relations.put(tex, new float[] {	//TODO investigate strange texture coordss
					leftX, bottomY, //top left
					leftX, 0f, 		//bottom left
					rightX, 0f, 	//bottom right
					rightX, bottomY	//top right
			});
			pixelX += tex.getWidth();
		}
		
		atlas.flip();
		
		size = widthPixels;
		return atlas;
	}
	
	private static void place(TextureData texture, ByteBuffer dest, int drow, int dcol, int dwidth, int bytesPerPixel) {
		byte[] data = texture.getData();
		int swidth = texture.getWidth() * bytesPerPixel, sheight = texture.getHeight();
		for(int srow = 0; srow < sheight; srow++) { //for each row in the source data
			dest.position((drow + srow) * dwidth + dcol); //set destination position
			dest.put(data, srow * swidth, swidth); //put source data
		}
	}
	
	private static int calculateAtlasWidth(TextureData[] textures, int checkBytesPerPixel) {
		int widthPixels = 0;
		for(var tex : textures) {
			if(tex.getBytesPerPixel() != checkBytesPerPixel)
				throw new IllegalStateException("conflicting pixel formats");
			widthPixels += tex.getWidth();
		}
		return widthPixels;
	}
	
	public float[] getTextureCoordinates(TextureData texture) {
		return coordinates.get(texture);
	}
	
	public ByteBuffer getAtlasData() {
		return texture;
	}
	
	public int toTexture() {
		return GraphicsUtility.uploadTextureData(texture, size, size);
	}
}
