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

	public static Font load(int glyphsPerRow, int glyphWidth, int glyphHeight, int paddingSize, int textureWidth, int textureHeight, Collection<ModelData> out,
		float[] pos, int[] indices, float startX, float startY, float endX, float endY) {
		return new Font(glyphsPerRow, glyphWidth, glyphHeight, paddingSize, textureWidth, textureHeight, out, pos, indices, startX, startY, endX, endY);
	}

	private static class CharModel implements Model {}

	private final CharModel[] glyphs;
	private static final CharModel UNKNOWN_CHAR_MODEL = null; //TODO create an unknown char model

	public Font(int glyphsPerRow, int glyphWidth, int glyphHeight, int paddingSize, int textureWidth, int textureHeight,
		Collection<ModelData> out, float[] pos, int[] indices, float leftX, float bottomY, float rightX, float topY) {
		glyphs = new CharModel[256];
		float width = (float)glyphWidth / textureWidth * (rightX - leftX);
		float height = (float)glyphHeight / textureHeight * (topY - bottomY);
		float paddingX = (float)paddingSize / textureWidth * (rightX - leftX);
		float paddingY = (float)paddingSize / textureHeight * (topY - bottomY);
		for(int i = 0; i < CHARACTER_LIST.length; i++) {
			int row = i / glyphsPerRow;
			int column = i % glyphsPerRow;
			float top = topY - paddingY - (paddingY + height) * row;
			float left = leftX + paddingX + (paddingX + width) * column;
			float bottom = top - height;
			float right = left + width;
			float[] coords = new float[] {left, top, left, bottom, right, bottom, right, top};
			out.add(new ModelData(glyphs[CHARACTER_LIST[i]] = new CharModel(), pos, coords, indices));
		}
	}

	public Model getModel(int character) {
		return character < glyphs.length ? glyphs[character] : UNKNOWN_CHAR_MODEL;
	}
}
