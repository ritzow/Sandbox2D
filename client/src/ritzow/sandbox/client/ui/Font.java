package ritzow.sandbox.client.ui;

import java.util.Collection;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.graphics.ModelRenderProgramBase.ModelData;

public final class Font {

	private static final char[] CHARACTER_LIST = new char[]{
		'!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		':', ';', '<', '=', '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
		'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', '\\', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
		'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', ' ',
	};

	//TODO some font characters are not in correct ascii order, such as the space character

	public static Font load(double texelWidth, int glyphsPerRow, int glyphWidth, int glyphHeight, int paddingSize,
		int textureWidth, int textureHeight, Collection<ModelData> out, int[] indices, float startX, float startY, float endX, float endY) {
		return new Font(texelWidth, glyphsPerRow, glyphWidth, glyphHeight, paddingSize, textureWidth, textureHeight, out, indices, startX, startY, endX, endY);
	}

	private final Model[] glyphs;
	private final int glyphWidth, glyphHeight;

	public int getGlyphWidth() {
		return glyphWidth;
	}

	public int getGlyphHeight() {
		return glyphHeight;
	}

	public Font(double texelWidth, int glyphsPerRow, int glyphWidth, int glyphHeight, int paddingSize, int textureWidth, int textureHeight,
		Collection<ModelData> out, int[] indices, float leftX, float bottomY, float rightX, float topY) {
		glyphs = new Model[256];
		this.glyphWidth = glyphWidth;
		this.glyphHeight = glyphHeight;
		double texelWidthHalf = 0;//texelWidth/4d/textureWidth;
		double texelHeightHalf = 0;//texelWidth/4d/textureHeight;
		double width = (double)glyphWidth / textureWidth * (rightX - leftX);
		double height = (double)glyphHeight / textureHeight * (topY - bottomY);
		double paddingX = (double)paddingSize / textureWidth * (rightX - leftX);
		double paddingY = (double)paddingSize / textureHeight * (topY - bottomY);
		for(int i = 0; i < CHARACTER_LIST.length; i++) {
			int row = i / glyphsPerRow;
			int column = i % glyphsPerRow;
			float top = (float)(topY - paddingY - (paddingY + height) * row - texelHeightHalf);
			float left = (float)(leftX + paddingX + (paddingX + width) * column + texelWidthHalf);
			float bottom = (float)(top - height + texelHeightHalf);
			float right = (float)(left + width - texelWidthHalf);
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
			out.add(new ModelData(model -> glyphs[CHARACTER_LIST[charIndex]] = model, glyphWidth, glyphHeight, positions, coords, indices));
		}
	}

	public Model getModel(int character) {
		return character < glyphs.length && character > 0 ? glyphs[character] : null;
	}
}
