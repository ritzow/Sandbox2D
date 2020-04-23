package ritzow.sandbox.client.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.graphics.OpenGLTexture;
import ritzow.sandbox.client.graphics.Textures;

public final class Font {

	private static final char[] CHARACTER_LIST = new char[] {
			'!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			':', ';', '<', '=', '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
			'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', '\\', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
			'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', ' ',
	};

	private final int[] glyphs;
	private final OpenGLTexture charsetLatin;
	private final String name;

	private static final CharModel UNKNOWN_CHAR_MODEL = new CharModel(-1);

	/** A class for loading and managing fonts, should only be used in GraphicsManager
	 * @throws IOException if unable to load texture
	 * **/
	public Font(File directory) throws IOException {
		File info = new File(directory, "info.txt");
		File latin = new File(directory, "sheet01.png");

		glyphs = new int[200];

		if(info.exists() && info.canRead()) {
			try(Scanner reader = new Scanner(info)) {
				this.name = reader.nextLine();
			}
		} else {
			throw new IOException("Unable to access info.txt file in font");
		}

		if(latin.exists() && latin.canRead()) {
			try(var in = new FileInputStream(latin)) {
				charsetLatin = Textures.loadTexture(in);
			}
		} else {
			throw new IOException("Font directory does not contain character sheet 1");
		}

		loadCharacters();
	}

	public String getName() {
		return name;
	}

	public void delete() {
		charsetLatin.delete();
	}

	private static record CharModel(int c) implements Model {}

	public Model getModel(char c) {
		return c < glyphs.length ? new CharModel(glyphs[c]) : UNKNOWN_CHAR_MODEL;
	}

	protected void loadCharacters() {
		float textureWidth = 0.03515625f;
		float textureHeight = 0.03515625f;
		float horizontalPadding = 0.00390626f; //exact 0.00390625f, use 0.00390626f to fix texture edges
		float verticalPadding = 0.00390625f;

		for(int i = 0; i < CHARACTER_LIST.length; i++) {
			loadCharacter(CHARACTER_LIST[i], charsetLatin, getTextureCoordinates(textureWidth, textureHeight, horizontalPadding, verticalPadding, i));
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
