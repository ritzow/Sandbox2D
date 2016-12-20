package ritzow.solomon.engine.resource;

import java.io.IOException;
import ritzow.solomon.engine.graphics.Model;
import ritzow.solomon.engine.graphics.data.IndexBuffer;
import ritzow.solomon.engine.graphics.data.PositionBuffer;
import ritzow.solomon.engine.graphics.data.TextureCoordinateBuffer;

public final class Models {
	
	public static Model GRASS_MODEL;
	public static Model DIRT_MODEL;
	public static Model CLOUDS;
	public static Model GREEN_FACE;
	public static Model BLUE_SQUARE;
	public static Model RED_SQUARE;
	
	public static PositionBuffer SQUARE_POSITIONS_BUFFER;
	public static IndexBuffer RECTANGLE_INDICES_BUFFER;
	public static TextureCoordinateBuffer FULL_TEXTURE_COORDINATES;
	
	public static void loadAll() throws IOException {
		
		float[] positions = {
				-0.5f,	 0.5f,
				-0.5f,	-0.5f,
				0.5f,	-0.5f,
				0.5f,	 0.5f,
		};

		float[] textureCoords = {
				0, 1,
				0, 0,
				1, 0,
				1, 1,
		};

		int[] indices = {
				0, 1, 2, 0, 2, 3
		};
		
		SQUARE_POSITIONS_BUFFER = new PositionBuffer(positions);
		RECTANGLE_INDICES_BUFFER = new IndexBuffer(indices);
		FULL_TEXTURE_COORDINATES = new TextureCoordinateBuffer(textureCoords);
		
		BLUE_SQUARE = new Model(6, SQUARE_POSITIONS_BUFFER, Textures.BLUE_SQUARE, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		RED_SQUARE = new Model(6, SQUARE_POSITIONS_BUFFER, Textures.RED_SQUARE, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		DIRT_MODEL = new Model(6, SQUARE_POSITIONS_BUFFER, Textures.DIRT, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		GREEN_FACE = new Model(6, SQUARE_POSITIONS_BUFFER, Textures.GREEN_FACE, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		GRASS_MODEL = new Model(6, SQUARE_POSITIONS_BUFFER, Textures.GRASS, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
		CLOUDS = new Model(6, SQUARE_POSITIONS_BUFFER, Textures.CLOUDS, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER);
	}
	
	public static void deleteAll() {
		GRASS_MODEL.delete();
		DIRT_MODEL.delete();
		CLOUDS.delete();
		GREEN_FACE.delete();
		BLUE_SQUARE.delete();
		RED_SQUARE.delete();
	}
}
