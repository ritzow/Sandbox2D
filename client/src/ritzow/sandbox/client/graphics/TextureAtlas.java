package ritzow.sandbox.client.graphics;

import java.util.Arrays;

public class TextureAtlas {
	private final byte[] image;
	
	public TextureAtlas(TextureImage... textures) {
		TextureImage[] texturesSorted = textures.clone();
		Arrays.sort(texturesSorted);
		
		//accumulate total width and height of final texture atlas
		int finalWidth = 0, finalHeight = 0;
		for(TextureImage t : textures) {
			if(t.getBytesPerPixel() != texturesSorted[0].getBytesPerPixel())
				throw new IllegalArgumentException("mismatch between texture formats (bytes per pixel not the same)");
			//TODO compute atlas size in bytes, method for doing so will need to be decided
		}
		byte[] atlas = new byte[finalWidth * finalHeight];
		
		//int startIndex;
		for(TextureImage t : textures) {
			int height = t.getBytesPerPixel()/t.getWidth();
			//int columnSize = t.getBytesPerPixel() * t.getWidth();
			for(int row = 0; row < height; row++) {
				for(int column = 0; column < t.getWidth(); column++) {
					//System.arraycopy(t.getData(), row * t.getWidth() + column, atlas, destPos, t.get);
				}
			}
		}
		image = atlas;
	}
	
//	private static void copyInto(byte[] dest, int row, int column) {
//		
//	}
	
	public byte[] getData() {
		return image;
	}
	
//	private static float get(float[] array, int row, int column) {
//		return array[(row * 4) + column];
//	}
	
//	private static void set(float[] array, int row, int column, float value) {
//		array[(row * 4) + column] =  value;
//	}
}
