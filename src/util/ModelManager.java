package util;

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
		
		PositionBuffer squarePositionsBuffer = new PositionBuffer(squarePositions);
		TextureCoordinateBuffer squareTexCoordsBuffer = new TextureCoordinateBuffer(textureCoordinatesEntireImage);
		IndexBuffer rectangleIndicesBuffer = new IndexBuffer(rectangleIndices);
		
		Texture greenFaceTexture = 	new Texture(directory + "textures/greenFace.png");
		Texture redBlockTexture = 	new Texture(directory + "textures/redSquare.png");
		Texture blueBlockTexture = 	new Texture(directory + "textures/blueSquare.png");
		Texture dirtTexture = 		new Texture(directory + "textures/dirt.png");
		Texture grassTexture = 		new Texture(directory + "textures/grass.png");
		Texture cloudsTexture = 	new Texture(directory + "textures/clouds.png");
		
		BLUE_SQUARE = new Model(squarePositionsBuffer, blueBlockTexture, squareTexCoordsBuffer, rectangleIndicesBuffer);
		RED_SQUARE = new Model(squarePositionsBuffer, redBlockTexture, squareTexCoordsBuffer, rectangleIndicesBuffer);
		DIRT_MODEL = new Model(squarePositionsBuffer, dirtTexture, squareTexCoordsBuffer, rectangleIndicesBuffer);
		GREEN_FACE = new Model(squarePositionsBuffer, greenFaceTexture, squareTexCoordsBuffer, rectangleIndicesBuffer);
		GRASS_MODEL = new Model(squarePositionsBuffer, grassTexture, squareTexCoordsBuffer, rectangleIndicesBuffer);
		CLOUDS_BACKGROUND = new Model(squarePositionsBuffer, cloudsTexture, squareTexCoordsBuffer, rectangleIndicesBuffer);
	}
	
	public static void loadFontModels() {
		
	}
	
	public static Model lookupCharacter(char c) {
		if((int)c < characters.length && characters[(int)c] != null) {
			return characters[(int)c];
		}
		
		else {
			return ModelManager.RED_SQUARE; //return an error model
		}
	}
	
	public static float[] getTextureCoordinates(float textureWidth, float textureHeight, float horizontalPadding, float verticalPadding, int index) {
		float[] coords = new float[8];
		
		
		
		
		return coords;
	}
}
