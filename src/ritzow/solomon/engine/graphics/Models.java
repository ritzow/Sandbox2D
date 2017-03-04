package ritzow.solomon.engine.graphics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Models {
	
	private Models() {}

	public static final int
		GRASS_INDEX = 0,
		DIRT_INDEX = 1,
		CLOUDS_INDEX = 2,
		GREEN_FACE_INDEX = 3,
		BLUE_SQUARE_INDEX = 4,
		RED_SQUARE_INDEX = 5;
	
	private static final Map<Integer, Model> models = new HashMap<Integer, Model>(6);
	
	public static Model forIndex(int modelIndex) {
		return models.get(modelIndex);
	}
	
	public static void loadAll() throws IOException {
		
		IndexBuffer RECTANGLE_INDICES_BUFFER = new IndexBuffer(new int[] {
				0, 1, 2, 0, 2, 3
		});
		
		PositionBuffer SQUARE_POSITIONS_BUFFER = new PositionBuffer(new float[] {
			-0.5f,	 0.5f,
			-0.5f,	-0.5f,
			0.5f,	-0.5f,
			0.5f,	 0.5f
		});
		
		TextureCoordinateBuffer FULL_TEXTURE_COORDINATES = new TextureCoordinateBuffer(new float[] {
				0, 1,
				0, 0,
				1, 0,
				1, 1,
		});

		models.put(GRASS_INDEX, new Model(6, RECTANGLE_INDICES_BUFFER.id, SQUARE_POSITIONS_BUFFER.id, FULL_TEXTURE_COORDINATES.id, Textures.GRASS.id));
		models.put(DIRT_INDEX, new Model(6, RECTANGLE_INDICES_BUFFER.id, SQUARE_POSITIONS_BUFFER.id, FULL_TEXTURE_COORDINATES.id, Textures.DIRT.id));
		models.put(GREEN_FACE_INDEX, new Model(6, RECTANGLE_INDICES_BUFFER.id, SQUARE_POSITIONS_BUFFER.id, FULL_TEXTURE_COORDINATES.id, Textures.GREEN_FACE.id));
		models.put(CLOUDS_INDEX, new Model(6, RECTANGLE_INDICES_BUFFER.id, SQUARE_POSITIONS_BUFFER.id, FULL_TEXTURE_COORDINATES.id, Textures.CLOUDS.id));
		models.put(RED_SQUARE_INDEX, new Model(6, RECTANGLE_INDICES_BUFFER.id, SQUARE_POSITIONS_BUFFER.id, FULL_TEXTURE_COORDINATES.id, Textures.RED_SQUARE.id));
	}
}
