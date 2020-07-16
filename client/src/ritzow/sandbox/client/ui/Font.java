package ritzow.sandbox.client.ui;

import java.util.HashMap;
import java.util.Map;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.graphics.OpenGLTexture;

public final class Font {

	private static final char[] CHARACTER_LIST = new char[] {
			'!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			':', ';', '<', '=', '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
			'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', '\\', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
			'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', ' ',
	};

	private final int[] glyphs;

	private final Map<Integer, CharModel> models;

//	private static final CharModel UNKNOWN_CHAR_MODEL = new CharModel(-1);

	public Font(float startX, float startY, float endX, float endY) {
		glyphs = new int[CHARACTER_LIST.length];
		models = new HashMap<>(200);
		loadCharacters(startX, startY, endX, endY);
	}

	private static record CharModel(float[] coords) implements Model {}

	public Model getModel(char c) {
//		return c < glyphs.length ? new CharModel(glyphs[c]) : UNKNOWN_CHAR_MODEL;
		return null;
	}

	protected void loadCharacters(float startX, float startY, float endX, float endY) {
		float textureWidth = 0.03515625f;
		float textureHeight = 0.03515625f;
		float horizontalPadding = 0.00390626f; //exact 0.00390625f, use 0.00390626f to fix texture edges
		float verticalPadding = 0.00390625f;

		for(int i = 0; i < CHARACTER_LIST.length; i++) {
			models.put(i, new CharModel(
				getTextureCoordinates(textureWidth, textureHeight, horizontalPadding, verticalPadding, i)
			));
		}
	}

	@SuppressWarnings("static-method")
	protected void loadCharacter(char c, OpenGLTexture characterSheet, float[] textureCoordinates) {
		throw new UnsupportedOperationException("character loading needs to be rewritten");
		//glyphs[(int)c] = new Model(6, Models.SQUARE_POSITIONS_BUFFER, characterSheet, new TextureCoordinateBuffer(textureCoordinates), Models.RECTANGLE_INDICES_BUFFER);
	}

	protected static float[] getTextureCoordinates(float textureWidth, float textureHeight, float horizontalPadding, float verticalPadding, int index) {
		int charsPerRow = (int)Math.floor(1.0f / (horizontalPadding + textureWidth));
		int row = index / charsPerRow;
		int column = index % charsPerRow;

		float top = 1 - (verticalPadding + (textureHeight + verticalPadding) * row);
		float left = horizontalPadding + (textureWidth + horizontalPadding) * column;
		float bottom = top - textureHeight;
		float right = left + textureWidth;

		return new float[] {left, top, left, bottom, right, bottom, right, top};
	}
}
