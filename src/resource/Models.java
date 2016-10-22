package resource;

import graphics.Model;
import graphics.data.IndexBuffer;
import graphics.data.PositionBuffer;
import graphics.data.Texture;
import graphics.data.TextureCoordinateBuffer;
import java.io.File;

public final class Models {
	
	public static Model GRASS_MODEL;
	public static Model DIRT_MODEL;
	public static Model CLOUDS_BACKGROUND;
	public static Model GREEN_FACE;
	public static Model BLUE_SQUARE;
	public static Model RED_SQUARE;
	
	public static Font DEFAULT_FONT;
	
	public static PositionBuffer SQUARE_POSITIONS_BUFFER;
	public static IndexBuffer RECTANGLE_INDICES_BUFFER;
	public static TextureCoordinateBuffer FULL_TEXTURE_COORDINATES;
	
	public static void loadAll(String directory) {
		
		if(!directory.endsWith("/")) {
			directory += "/";
		}
		System.out.println(directory);
		
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
				0, 1, 2, 0, 2, 3
		};
		
		SQUARE_POSITIONS_BUFFER = new PositionBuffer(squarePositions);
		RECTANGLE_INDICES_BUFFER = new IndexBuffer(rectangleIndices);
		FULL_TEXTURE_COORDINATES = new TextureCoordinateBuffer(textureCoordinatesEntireImage);
		
		Texture greenFaceTexture = 	new Texture(directory + "greenFace.png");
		Texture redBlockTexture = 	new Texture(directory + "redSquare.png");
		Texture blueBlockTexture = 	new Texture(directory + "blueSquare.png");
		Texture dirtTexture = 		new Texture(directory + "dirt.png");
		Texture grassTexture = 		new Texture(directory + "grass.png");
		Texture cloudsTexture = 	new Texture(directory + "clouds.png");
		
		(DEFAULT_FONT = new Font(new File(directory + "fonts/default"))).load();
		
		BLUE_SQUARE = new Model(SQUARE_POSITIONS_BUFFER, blueBlockTexture, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		RED_SQUARE = new Model(SQUARE_POSITIONS_BUFFER, redBlockTexture, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		DIRT_MODEL = new Model(SQUARE_POSITIONS_BUFFER, dirtTexture, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		GREEN_FACE = new Model(SQUARE_POSITIONS_BUFFER, greenFaceTexture, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		GRASS_MODEL = new Model(SQUARE_POSITIONS_BUFFER, grassTexture, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		CLOUDS_BACKGROUND = new Model(SQUARE_POSITIONS_BUFFER, cloudsTexture, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
	}
}
