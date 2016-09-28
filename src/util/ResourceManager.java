package util;

import graphics.Model;
import graphics.data.*;
import java.util.HashMap;
import java.util.Map.Entry;

public final class ResourceManager {	
	
	private static HashMap<String, Model> models;
	
	static {
		models = new HashMap<String, Model>();
	}
	
	public static void loadResources(String directory) {
		
		float[] squarePositions = {
				-0.5f,	 0.5f,
				-0.5f,	-0.5f,
				0.5f,	-0.5f,
				0.5f,	 0.5f,
		};
		
//		float[] creatureArm = {
//				-0.25f, 0.25f,
//				-0.25f, -0.25f,
//				0.75f, -0.25f,
//				0.75f, 0.25f
//		};

		float[] textureCoordinatesEntireImage = {
				0, 1,
				0, 0,
				1, 0,
				1, 1,
		};
		
//		float[] creatureArmTexture = {
//				0, 1,
//				0, 0.5f,
//				1, 0.5f,
//				1, 1,
//		};

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

		Texture greenFaceTexture = new Texture(directory + "textures/greenFace.png");
		Texture redBlockTexture = new Texture(directory + "textures/redSquare.png");
		Texture blueBlockTexture = new Texture(directory + "textures/blueSquare.png");
		Texture dirtTexture = new Texture(directory + "textures/dirt.png");
		Texture grassTexture = new Texture(directory + "textures/grass.png");

		IndexBuffer rectangleIndicesBuffer = new IndexBuffer(rectangleIndices);
		
		ResourceManager.add("green_face", new Model(squarePositionsBuffer, greenFaceTexture, squareTexCoordsBuffer, rectangleIndicesBuffer));
		ResourceManager.add("red_square", new Model(squarePositionsBuffer, redBlockTexture, squareTexCoordsBuffer, rectangleIndicesBuffer));
		ResourceManager.add("blue_square", new Model(squarePositionsBuffer, blueBlockTexture, squareTexCoordsBuffer, rectangleIndicesBuffer));
		ResourceManager.add("dirt", new Model(squarePositionsBuffer, dirtTexture, squareTexCoordsBuffer, rectangleIndicesBuffer));
		ResourceManager.add("grass", new Model(squarePositionsBuffer, grassTexture, squareTexCoordsBuffer, rectangleIndicesBuffer));
	}
	
	public static void deleteAll() {
		for(Entry<String, Model> entry : models.entrySet()) {
			Model model = entry.getValue();
			
			if(model != null) {
				model.delete();
			}
		}
		
		models.clear();
	}
	
	public static void add(String name, Model model) {
		models.put(name, model);
	}
	
	public static void deleteModel(String name) {
		Model model = models.remove(name);
		
		if(model != null) {
			model.delete();
		}
	}
	
	public static Model getModel(String name) {
		return models.get(name);
	}
}
