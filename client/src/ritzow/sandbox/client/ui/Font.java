package ritzow.sandbox.client.ui;

import java.util.function.Consumer;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.graphics.ModelRenderProgramBase.ModelData;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.graphics.TextureAtlas;
import ritzow.sandbox.client.graphics.TextureAtlas.AtlasRegion;
import ritzow.sandbox.client.graphics.TextureData;

public final class Font {

	private static final char[] CHARACTER_LIST = {
		'!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		':', ';', '<', '=', '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
		'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', '\\', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
		'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', ' ',
	};

	//TODO some font characters are not in correct ascii order, such as the space character

	public static Font load(double texelWidth, int glyphsPerRow, int glyphWidth, int glyphHeight, int paddingSize,
		int textureWidth, int textureHeight, Consumer<ModelData> out, int[] indices, float startX, float startY, float endX, float endY) {
		return new Font(texelWidth, glyphsPerRow, glyphWidth, glyphHeight, paddingSize, textureWidth, textureHeight, out, indices, startX, startY, endX, endY);
	}

	public static Font load(TextureData fontTexture, TextureAtlas atlas, AtlasRegion region, Consumer<ModelData> modelOut) {
		return new Font(1.0d / atlas.width(), 25, 9, 9, 1, fontTexture.getWidth(), fontTexture.getHeight(), modelOut,
			RenderManager.VERTEX_INDICES_RECT, region.leftX(), region.bottomY(), region.rightX(), region.topY());
	}

	private final Model[] glyphs;
	private final int glyphWidth, glyphHeight;

	public int getGlyphWidth() {
		return glyphWidth;
	}

	public int getGlyphHeight() {
		return glyphHeight;
	}

	public Font(double texelDimension, int glyphsPerRow, int glyphWidth, int glyphHeight, int paddingSize, int textureWidth, int textureHeight,
		Consumer<ModelData> out, int[] indices, float leftX, float bottomY, float rightX, float topY) {
		glyphs = new Model[256];
		this.glyphWidth = glyphWidth;
		this.glyphHeight = glyphHeight;
		double texelWidthHalf = texelDimension/textureWidth/2d;
		double texelHeightHalf = texelDimension/textureHeight/2d;
		double width = (double)glyphWidth / textureWidth * (rightX - leftX);
		double height = (double)glyphHeight / textureHeight * (topY - bottomY);
		double paddingX = (double)paddingSize / textureWidth * (rightX - leftX);
		double paddingY = (double)paddingSize / textureHeight * (topY - bottomY);
		for(int i = 0; i < CHARACTER_LIST.length; i++) {
			int row = i / glyphsPerRow;
			int column = i % glyphsPerRow;
			double topInitial = topY - paddingY - (paddingY + height) * row;
			double leftInitial = leftX + paddingX + (paddingX + width) * column;
			float top = (float)(topInitial - texelHeightHalf);
			float left = (float)(leftInitial + texelWidthHalf);
			float bottom = (float)(topInitial - height + texelHeightHalf);
			float right = (float)(leftInitial + width - texelWidthHalf);

			float[] coords = new float[] {
				left, top,
				left, bottom,
				right, bottom,
				right, top
			};

			float halfWidth = (float)(glyphWidth/2d);
			float halfHeight = (float)(glyphHeight/2d);
			float[] positions = {
				-halfWidth,	 halfHeight,
				-halfWidth,	-halfHeight,
				halfWidth,	-halfHeight,
				halfWidth,	 halfHeight
			};

			int charIndex = i;
			out.accept(new ModelData(model -> glyphs[CHARACTER_LIST[charIndex]] = model, glyphWidth, glyphHeight, positions, coords, indices));
		}
	}

	public Model getModel(int character) {
		return character < glyphs.length && character > 0 ? glyphs[character] : null;
	}
}
