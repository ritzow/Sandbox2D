package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;

public class TextureAtlas {
	private final Map<TextureData, float[]> coordinates;
	private final int texture;
	
	TextureAtlas(TextureData... textures) {
		texture = generateAtlas(textures, coordinates = new HashMap<TextureData, float[]>());
	}
	
	public float[] getCoordinates(TextureData texture) {
		return coordinates.get(texture);
	}
	
	public int texture() {
		return texture;
	}
	
	private static int generateAtlas(TextureData[] textures, Map<TextureData, float[]> coords) {
		//TODO for now just put them all in a horizontal line
		int bytesPerPixel = textures[0].getBytesPerPixel();
		int atlasSize = calculateAtlasWidth(textures, bytesPerPixel);
		
		//create the atlas buffer
		ByteBuffer atlas = BufferUtils.createByteBuffer(atlasSize * atlasSize * bytesPerPixel);
		
		//place the textures in the atlas
		int pixelX = 0;
		for(TextureData tex : textures) {
			coords.put(tex, placeTexture(tex, atlas, pixelX, 0, atlasSize));
			pixelX += tex.getWidth();
		}
		
		//fill rest of texture with purple
		while(atlas.hasRemaining()) {
			atlas.put((byte)255);
			atlas.put((byte)0);
			atlas.put((byte)255);
			atlas.put((byte)255);
		}
		
		//ready the atlas data for reading and upload to gpu
		atlas.flip();
		return GraphicsUtility.uploadTextureData(atlas, atlasSize, atlasSize);
	}
	
	private static float[] placeTexture(TextureData texture, ByteBuffer dest, int pixelX, int pixelY, int atlasWidthPixels) {
		int bpp = texture.getBytesPerPixel();
		place(texture.getData(),
				texture.getWidth() * bpp,
				texture.getHeight(),
				dest,
				pixelY,
				pixelX * bpp, 
				atlasWidthPixels * bpp);
		return convertCoordinates(texture, pixelX, pixelY, atlasWidthPixels);
	}
	
	private static float[] convertCoordinates(TextureData tex, int pixelX, int pixelY, int atlasWidthPixels) {
		float size = atlasWidthPixels;
		float leftX = pixelX/size;
		float rightX = (pixelX + tex.getWidth())/size;
		float topY = pixelY/size;
		float bottomY =  (pixelY + tex.getHeight())/size;
		
		return new float[] {//TODO investigate strange/flipped texture coords
				leftX, bottomY, //top left
				leftX, topY, 		//bottom left
				rightX, topY, 	//bottom right
				rightX, bottomY	//top right
		};
	}
	
	//uses byte indices, not pixels
	private static void place(byte[] tex, int twidth, int theight, ByteBuffer dest, int drow, int dcol, int dwidth) {
		for(int trow = 0; trow < theight; trow++) { //for each row in the source data
			dest.position((drow + trow) * dwidth + dcol); //set destination position
			dest.put(tex, trow * twidth, twidth); //put source data
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
}
