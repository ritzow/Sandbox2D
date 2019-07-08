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
	
	//TODO optimize texture packing https://gamedev.stackexchange.com/questions/2829/texture-packing-algorithm
	private static int generateAtlas(TextureData[] textures, Map<TextureData, float[]> coords) {
		int bytesPerPixel = textures[0].getBytesPerPixel();
		
		int totalWidth = 0, height = 0;
		for(var tex : textures) {
			if(tex.getBytesPerPixel() != bytesPerPixel)
				throw new IllegalStateException("conflicting pixel formats");
			totalWidth += tex.getWidth();
			height = Math.max(height, tex.getHeight());
		}
		
		
		//create the atlas buffer
		int dimension = Math.max(totalWidth, height);
		ByteBuffer atlas = BufferUtils.createByteBuffer(dimension * dimension * bytesPerPixel);
		
		//initially fill the texture with purple
		while(atlas.hasRemaining()) {
			atlas.put((byte)255);
			atlas.put((byte)0);
			atlas.put((byte)255);
			atlas.put((byte)255);
		}
		
		//Three things to keep in mind:
		//PNGDecoder format: first element is upper left
		//OpenGL format: first element is lower left
		//UV Coord format: lower left is 0,0
		
		//place the textures in the atlas
		//"The first element corresponds to the lower left corner of the texture image. 
		//Subsequent elements progress left-to-right through the remaining texels in 
		//the lowest row of the texture image, and then in successively higher rows of
		//the texture image. The final element corresponds to the upper right corner of the texture image."
		//https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glTexImage2D.xhtml
		int pixelX = 0;
		for(TextureData tex : textures) {
			//(int)(Math.random() * (dimension - tex.getHeight()))
			coords.put(tex, placeTexture(tex, atlas, pixelX, 0, dimension));
			pixelX += tex.getWidth();
		}
		
		//ready the atlas data for reading and upload to gpu
		atlas.flip();
		return GraphicsUtility.uploadTextureData(atlas, dimension, dimension);
	}
	
	private static final float OFFSET = 0.000005f;
	
	//TODO fix having to add small offsets to fix texture bleeding
	private static float[] convertCoordinates(TextureData tex, int pixelX, int pixelY, int atlasWidthPixels) {
		float size = atlasWidthPixels;
		float leftX = (pixelX)/size + OFFSET;
		float rightX = (pixelX + tex.getWidth())/size - OFFSET;
		float bottomY =  (size - pixelY - tex.getHeight())/size + OFFSET;
		float topY = (size - pixelY)/size - OFFSET;
		
		return new float[] {
				leftX, topY, //top left
				leftX, bottomY, //bottom left
				rightX, bottomY,	//bottom right
				rightX, topY 	//top right
		};
	}
	
	private static float[] placeTexture(TextureData texture, ByteBuffer dest, int pixelX, int pixelY, int atlasWidthPixels) {
		int bpp = texture.getBytesPerPixel();
		place(texture.getData(),texture.getWidth() * bpp,texture.getHeight(),dest,pixelY,pixelX * bpp, atlasWidthPixels * bpp);
		return convertCoordinates(texture, pixelX, pixelY, atlasWidthPixels);
	}
	
	//uses byte indices, not pixels
	//0,0 is upper left
	private static void place(byte[] tex, int twidth, int theight, ByteBuffer dest, int drow, int dcol, int dwidth) {
		drow = dest.capacity()/dwidth - drow - 1; //invert the row so the top is the bottom
		for(int trow = 0; trow < theight; trow++) { //for each row in the source data (0,0 top left)
			dest.position((drow - trow) * dwidth + dcol); //set destination position
			dest.put(tex, trow * twidth, twidth); //put source data
		}
	}
}
