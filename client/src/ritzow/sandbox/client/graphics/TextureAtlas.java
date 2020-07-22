package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;

public class TextureAtlas {
	private final Map<TextureData, AtlasRegion> coordinates;
	private final int texture;
	private int dimension;

	public static final float[] ATLAS_COORDS = new float[] {
		0f, 1f,
		0f, 0f,
		1f, 0f,
		1f, 1f
	};

	public static final float[] NORMAL_POS = {
		-0.5f,	 0.5f,
		-0.5f,	-0.5f,
		0.5f,	-0.5f,
		0.5f,	 0.5f
	};

	//TODO maybe try https://stackoverflow.com/a/9251578 (similar to what I already did, a bit of a hack)

	TextureAtlas(TextureData... textures) {
		texture = generateAtlas(textures, coordinates = new HashMap<>());
	}

	public AtlasRegion getRegion(TextureData texture) {
		return coordinates.get(texture);
	}

	public static record AtlasRegion(float leftX, float rightX, float bottomY, float topY) {

		/**
		 * Texture coordinates are in OpenGL UV format as floats
		 * in the range [0, 1) where {0, 0} is the bottom left corner. An array of format
		 * {leftX, topY, leftX, bottomY, rightX, bottomY, rightX, topY}
		 * @return Texture coordinates in the above specified order
		 */
		public float[] toTextureCoordinates() {
			return new float[] {
				leftX, topY, //top left
				leftX, bottomY, //bottom left
				rightX, bottomY,	//bottom right
				rightX, topY 	//top right
			};
		}
	}

	public int texture() {
		return texture;
	}

	public int width() {
		return dimension;
	}

	//TODO optimize texture packing https://gamedev.stackexchange.com/questions/2829/texture-packing-algorithm
	private int generateAtlas(TextureData[] textures, Map<TextureData, AtlasRegion> coords) {
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
		this.dimension = dimension;
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

	//TODO fix having to add small offsets to fix texture bleeding
	private static AtlasRegion convertCoordinates(TextureData tex, int pixelX, int pixelY, int atlasWidthPixels) {
		//TODO texel offset did not work, still happens in large worlds
		double texelWidthHalf = 0.25d/atlasWidthPixels; //for some reason 0.25 (quarter pixel?) works as well or better than 0.5?
		//OpenGL texture coordinates range from 0 to 1, with the origin in the bottom left
		//TODO this causes some slightly incorrect pixel sizes, but removes most errors (still some gaps in places), might be possible to improve?
		//TODO do the same half texel thing with the texture atlas.
		float leftX = (float)((pixelX + texelWidthHalf)/atlasWidthPixels);
		float rightX = (float)((pixelX + tex.getWidth() - texelWidthHalf)/atlasWidthPixels);
		float bottomY = (float)((atlasWidthPixels - pixelY - tex.getHeight() + texelWidthHalf)/atlasWidthPixels);
		float topY = (float)((atlasWidthPixels - pixelY - texelWidthHalf)/atlasWidthPixels);
		return new AtlasRegion(leftX, rightX, bottomY, topY);
	}

	private static AtlasRegion placeTexture(TextureData texture,
			ByteBuffer dest, int pixelX, int pixelY, int atlasWidthPixels) {
		int bpp = texture.getBytesPerPixel();
		place(texture.getData(),texture.getWidth() * bpp,texture.getHeight(),
				dest,pixelY,pixelX * bpp, atlasWidthPixels * bpp);
		return convertCoordinates(texture, pixelX, pixelY, atlasWidthPixels);
	}

	//uses byte indices, not pixels
	//0,0 is upper left
	private static void place(byte[] tex,
		int twidth, int theight, ByteBuffer dest, int drow, int dcol, int dwidth) {
		drow = dest.capacity()/dwidth - drow - 1; //invert the row so the top is the bottom
		for(int trow = 0; trow < theight; trow++) { //for each row in the source data (0,0 top left)
			dest.position((drow - trow) * dwidth + dcol); //set destination position
			dest.put(tex, trow * twidth, twidth); //put source data
		}
	}
}
