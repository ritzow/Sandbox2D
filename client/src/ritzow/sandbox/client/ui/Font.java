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
		int textureWidth, int textureHeight, Collection<ModelData> out, float[] pos, int[] indices, float startX, float startY, float endX, float endY) {
		return new Font(texelWidth, glyphsPerRow, glyphWidth, glyphHeight, paddingSize, textureWidth, textureHeight, out, pos, indices, startX, startY, endX, endY);
	}

	private static class CharModel implements Model {}

	private final CharModel[] glyphs;

	public Font(double texelWidth, int glyphsPerRow, int glyphWidth, int glyphHeight, int paddingSize, int textureWidth, int textureHeight,
		Collection<ModelData> out, float[] pos, int[] indices, float leftX, float bottomY, float rightX, float topY) {
		glyphs = new CharModel[256];
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
			float[] coords = new float[] {left, top, left, bottom, right, bottom, right, top};
			out.add(new ModelData(glyphs[CHARACTER_LIST[i]] = new CharModel(), pos, coords, indices));
		}
	}

	public Model getModel(int character) {
		return character < glyphs.length && character > 0 ? glyphs[character] : null;
	}
}
