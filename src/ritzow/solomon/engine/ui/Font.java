package ritzow.solomon.engine.ui;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import ritzow.solomon.engine.graphics.Model;
import ritzow.solomon.engine.graphics.Models;
import ritzow.solomon.engine.graphics.Texture;
import ritzow.solomon.engine.graphics.TextureCoordinateBuffer;
import ritzow.solomon.engine.graphics.Textures;

public final class Font {
	
	protected Model[] characterModels;
	protected Texture charset1;
	protected String name;
	protected File directory;
	
	/** A class for loading and managing fonts, should only be used in GraphicsManager 
	 * @throws IOException if unable to load texture 
	 * **/
	public Font(File directory) throws IOException {
		this.directory = directory;
		characterModels = new Model[200];

		File sheet01 = new File(directory, "sheet01.png");
		
		if(sheet01.exists() && sheet01.canRead()) {
			charset1 = Textures.loadTexture(sheet01);
		} else {
			throw new IOException("Font directory does not contain character sheet 1");
		}
		
		loadInfo();
		loadCharacters();
	}
	
	public String getName() {
		return name;
	}
	
	public void delete() {
		for(Model model : characterModels) {
			if(model != null) {
				model.delete();
			}
		}
		
		charset1.delete();
	}
	
	public Model getModel(char c) {
		if((int)c < characterModels.length && characterModels[(int)c] != null) {
			return characterModels[(int)c];
		}
		
		else {
			return null; //return an error model TODO make a custom unknown character model
		}
	}
	
	protected void loadInfo() throws IOException {
		File info = new File(directory, "info.txt");
		if(info.exists() && info.canRead()) {
			Scanner reader = new Scanner(info);
			this.name = reader.nextLine();
			reader.close();
		} else {
			throw new IOException("Unable to access info.txt file in font");
		}
	}
	
	protected void loadCharacters() {
		float textureWidth = 0.03515625f;
		float textureHeight = 0.03515625f;
		float horizontalPadding = 0.00390626f; //exact 0.00390625f, use 0.00390626f to fix texture edges
		float verticalPadding = 0.00390625f;
		
		char[] characters = new char[] {
				'!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
				':', ';', '<', '=', '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
				'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', '\\', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
				'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', ' ',
		};
		
		for(int i = 0; i < characters.length; i++) {
			loadCharacter(characters[i], charset1, getTextureCoordinates(textureWidth, textureHeight, horizontalPadding, verticalPadding, i));
		}
	}
	
	protected void loadCharacter(char c, Texture characterSheet, float[] textureCoordinates) { //TODO why is vertex count six for character models?
		characterModels[(int)c] = new Model(6, Models.SQUARE_POSITIONS_BUFFER, characterSheet, new TextureCoordinateBuffer(textureCoordinates), Models.RECTANGLE_INDICES_BUFFER);
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
