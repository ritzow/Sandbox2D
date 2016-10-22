package resource;

import graphics.Model;
import graphics.data.Texture;
import graphics.data.TextureCoordinateBuffer;
import java.io.File;

public class Font {
	
	protected Model[] characterModels;
	
	protected Texture characterSheet01;
	
	/** A class for loading and managing fonts, should only be used in GraphicsManager **/
	public Font(File directory) {
		characterModels = new Model[200];
		
		File[] files = directory.listFiles();
		
		for(File f : files) {
			if(characterSheet01 == null && f.canRead() && f.getName().equals("sheet01.png")) {
				characterSheet01 = new Texture(f.getPath());
			}
		}
	}
	
	public Model getModel(char c) {
		if((int)c < characterModels.length && characterModels[(int)c] != null) {
			return characterModels[(int)c];
		}
		
		else {
			return Models.RED_SQUARE; //return an error model
		}
	}
	
	public void load() {
		float textureWidth = 0.03515625f;
		float textureHeight = 0.03515625f;
		float horizontalPadding = 0.00390626f; //exact 0.00390625f
		float verticalPadding = 0.00390625f;
		
		char[] characters = new char[] {
				'!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
				':', ';', '<', '=', '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
				'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', '\\', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
				'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', ' ',
		};
		
		for(int i = 0; i < characters.length; i++) {
			loadCharacter(characters[i], characterSheet01, getTextureCoordinates(textureWidth, textureHeight, horizontalPadding, verticalPadding, i));
		}
	}
	
	protected void loadCharacter(char c, Texture characterSheet, float[] textureCoordinates) {
		characterModels[(int)c] = new Model(Models.SQUARE_POSITIONS_BUFFER, characterSheet, new TextureCoordinateBuffer(textureCoordinates), Models.RECTANGLE_INDICES_BUFFER);
	}
	
	protected float[] getTextureCoordinates(float textureWidth, float textureHeight, float horizontalPadding, float verticalPadding, int index) {
		int charsPerRow = (int)Math.floor(1.0f / (horizontalPadding + textureWidth));
		int row = (int)(index / charsPerRow);
		int column = index % charsPerRow;
		
		float top = 1 - (verticalPadding + (textureHeight + verticalPadding) * row);
		float left = horizontalPadding + (textureWidth + horizontalPadding) * column;
		float bottom = top - textureHeight;
		float right = left + textureWidth;

		return new float[] {left, top, left, bottom, right, bottom, right, top};
	}
}
