package ritzow.solomon.engine.graphics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class Models {
	private static final Map<Integer, Model> models;
	
	public static final int 
		GRASS_INDEX = 0,
		DIRT_INDEX = 1,
		CLOUDS_INDEX = 2,
		GREEN_FACE_INDEX = 3,
		BLUE_SQUARE_INDEX = 4,
		RED_SQUARE_INDEX = 5;
	
	public static PositionBuffer SQUARE_POSITIONS_BUFFER;
	public static IndexBuffer RECTANGLE_INDICES_BUFFER;
	public static TextureCoordinateBuffer FULL_TEXTURE_COORDINATES;
	
	static {
		models = new HashMap<Integer, Model>();
	}
	
	public static Model get(int modelIndex) {
		return models.get(modelIndex);
	}
	
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
		
		models.put(GRASS_INDEX, new Model(6, SQUARE_POSITIONS_BUFFER, Textures.GRASS, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER));
		models.put(BLUE_SQUARE_INDEX, new Model(6, SQUARE_POSITIONS_BUFFER, Textures.BLUE_SQUARE, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER));
		models.put(DIRT_INDEX, new Model(6, SQUARE_POSITIONS_BUFFER, Textures.DIRT, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER));
		models.put(GREEN_FACE_INDEX, new Model(6, SQUARE_POSITIONS_BUFFER, Textures.GREEN_FACE, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER));
		models.put(CLOUDS_INDEX, new Model(6, SQUARE_POSITIONS_BUFFER, Textures.CLOUDS, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER));
		models.put(RED_SQUARE_INDEX, new Model(6, SQUARE_POSITIONS_BUFFER, Textures.RED_SQUARE, FULL_TEXTURE_COORDINATES, RECTANGLE_INDICES_BUFFER));
	}
	
	public static void deleteAll() {
		for(Entry<Integer, Model> e : models.entrySet()) {
			e.getValue().delete();
		}
		models.clear();
	}
}
