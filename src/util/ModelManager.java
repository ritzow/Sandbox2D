package util;

import java.util.Arrays;

import graphics.Model;
import graphics.data.IndexBuffer;
import graphics.data.PositionBuffer;
import graphics.data.Texture;
import graphics.data.TextureCoordinateBuffer;

public final class ModelManager {
	
	public static Model GRASS_MODEL;
	public static Model DIRT_MODEL;
	public static Model CLOUDS_BACKGROUND;
	public static Model GREEN_FACE;
	public static Model BLUE_SQUARE;
	public static Model RED_SQUARE;
	
	private static Model[] characters = new Model[200];
	
	private static PositionBuffer squarePositionsBuffer;
	private static IndexBuffer rectangleIndicesBuffer;
	private static TextureCoordinateBuffer fullTextureCoordsBuffer;
	
	private static Texture characterSheet01;
	
	public static void load(String directory) {
		
		float[] squarePositions = {
				-0.5f,	 0.5f,
				-0.5f,	-0.5f,
				0.5f,	-0.5f,
				0.5f,	 0.5f,
		};

		float[] textureCoordinatesEntireImage = {
				0, 1,
				0, 0,
				1, 0,
				1, 1,
		};

		int[] rectangleIndices = {
				0,
				1,
				2,
				0,
				2,
				3,	
		};
		
		squarePositionsBuffer = new PositionBuffer(squarePositions);
		rectangleIndicesBuffer = new IndexBuffer(rectangleIndices);
		fullTextureCoordsBuffer = new TextureCoordinateBuffer(textureCoordinatesEntireImage);
		
		Texture greenFaceTexture = 	new Texture(directory + "textures/greenFace.png");
		Texture redBlockTexture = 	new Texture(directory + "textures/redSquare.png");
		Texture blueBlockTexture = 	new Texture(directory + "textures/blueSquare.png");
		Texture dirtTexture = 		new Texture(directory + "textures/dirt.png");
		Texture grassTexture = 		new Texture(directory + "textures/grass.png");
		Texture cloudsTexture = 	new Texture(directory + "textures/clouds.png");
		
		characterSheet01 = new Texture(directory + "textures/font/font page 1.png");
		
		BLUE_SQUARE = new Model(squarePositionsBuffer, blueBlockTexture, fullTextureCoordsBuffer, rectangleIndicesBuffer);
		RED_SQUARE = new Model(squarePositionsBuffer, redBlockTexture, fullTextureCoordsBuffer, rectangleIndicesBuffer);
		DIRT_MODEL = new Model(squarePositionsBuffer, dirtTexture, fullTextureCoordsBuffer, rectangleIndicesBuffer);
		GREEN_FACE = new Model(squarePositionsBuffer, greenFaceTexture, fullTextureCoordsBuffer, rectangleIndicesBuffer);
		GRASS_MODEL = new Model(squarePositionsBuffer, grassTexture, fullTextureCoordsBuffer, rectangleIndicesBuffer);
		CLOUDS_BACKGROUND = new Model(squarePositionsBuffer, cloudsTexture, fullTextureCoordsBuffer, rectangleIndicesBuffer);
		
		loadFontModels();
	}
	
	public static void loadFontModels() {
		System.out.println(Arrays.toString(getTextureCoordinates(0.03515625f, 0.03515625f, 0.00390625f, 0.00390625f, 0)));
		loadCharacter('!', characterSheet01, getTextureCoordinates(0.03515625f, 0.03515625f, 0.00390625f, 0.00390625f, 0));
	}
	
	public static Model lookupCharacter(char c) {
		if((int)c < characters.length && characters[(int)c] != null) {
			return characters[(int)c];
		}
		
		else {
			return ModelManager.RED_SQUARE; //return an error model
		}
	}
	
	public static void loadCharacter(char c, Texture characterSheet, float[] textureCoordinates) {
		characters[(int)c] = new Model(squarePositionsBuffer, characterSheet, new TextureCoordinateBuffer(textureCoordinates), rectangleIndicesBuffer);
	}
	
	public static float[] getTextureCoordinates(float textureWidth, float textureHeight, float horizontalPadding, float verticalPadding, int index) {
		int charsPerRow = (int)Math.floor(1.0f / (horizontalPadding + textureWidth));
		row    = (int)(index / width);
		column = index % width;
		float top = 1 - verticalPadding;
		float bottom = 1 - (verticalPadding + textureHeight);
		float left = horizontalPadding;
		float right = horizontalPadding + textureWidth;
		System.out.println("character row: " + row);
		return new float[] {left, top, left, bottom, right, bottom, right, top};
	}
}
