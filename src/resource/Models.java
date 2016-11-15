package resource;

import graphics.Model;
import graphics.data.IndexBuffer;
import graphics.data.PositionBuffer;
import graphics.data.TextureCoordinateBuffer;
import java.io.File;
import java.io.IOException;

public final class Models {
	
	public static Model GRASS_MODEL;
	public static Model DIRT_MODEL;
	public static Model CLOUDS_BACKGROUND;
	public static Model GREEN_FACE;
	public static Model BLUE_SQUARE;
	public static Model RED_SQUARE;
	
	public static PositionBuffer SQUARE_POSITIONS_BUFFER;
	public static IndexBuffer RECTANGLE_INDICES_BUFFER;
	public static TextureCoordinateBuffer FULL_TEXTURE_COORDINATES;
	
	public static void loadAll(File directory) throws IOException {
		
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
		
		BLUE_SQUARE = new Model(SQUARE_POSITIONS_BUFFER, Textures.BLUE_SQUARE, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		RED_SQUARE = new Model(SQUARE_POSITIONS_BUFFER, Textures.RED_SQUARE, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		DIRT_MODEL = new Model(SQUARE_POSITIONS_BUFFER, Textures.DIRT, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		GREEN_FACE = new Model(SQUARE_POSITIONS_BUFFER, Textures.GREEN_FACE, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		GRASS_MODEL = new Model(SQUARE_POSITIONS_BUFFER, Textures.GRASS, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		CLOUDS_BACKGROUND = new Model(SQUARE_POSITIONS_BUFFER, Textures.CLOUDS, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
	}
	
	public static void deleteAll() {
		GRASS_MODEL.delete();
		DIRT_MODEL.delete();
		CLOUDS_BACKGROUND.delete();
		GREEN_FACE.delete();
		BLUE_SQUARE.delete();
		RED_SQUARE.delete();
	}
}
