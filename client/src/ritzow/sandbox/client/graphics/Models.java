package ritzow.sandbox.client.graphics;

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
		int indices = GraphicsUtility.uploadIndexData(0, 1, 2, 0, 2, 3);
		
		int positions = GraphicsUtility.uploadVertexData(
				-0.5f,	 0.5f,
				-0.5f,	-0.5f,
				0.5f,	-0.5f,
				0.5f,	 0.5f
		);
		
		int textureCoordinates = GraphicsUtility.uploadVertexData(
				0f, 1f,
				0f, 0f,
				1f, 0f,
				1f, 1f	
		);
		
		models.put(GRASS_INDEX,			new Model(6, indices, positions, textureCoordinates, Textures.GRASS.id));
		models.put(DIRT_INDEX,			new Model(6, indices, positions, textureCoordinates, Textures.DIRT.id));
		models.put(GREEN_FACE_INDEX,	new Model(6, indices, positions, textureCoordinates, Textures.GREEN_FACE.id));
		models.put(CLOUDS_INDEX,		new Model(6, indices, positions, textureCoordinates, Textures.CLOUDS.id));
		models.put(RED_SQUARE_INDEX,	new Model(6, indices, positions, textureCoordinates, Textures.RED_SQUARE.id));
		
		GraphicsUtility.checkErrors();
	}
}
