package ritzow.sandbox.client.graphics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL46C.*;

public class ModelStorage {
	protected static final int POSITION_SIZE = 2;
	protected static final int TEXTURE_COORD_SIZE = 2;

	static record ModelAttributes(
		int indexOffset,
		int indexCount, float width, float height) implements Model {}

	public static record ModelData(
		ModelDestination out,
		float width, float height,
		float[] positions,
		float[] textureCoords,
		int[] indices) {
		public int indexCount() {
			return indices.length;
		}
	}

	public static record ModelBuffers(int positionsVBO, int textureCoordsVBO, int indicesVBO) {}

	public static ModelBuffers uploadModels(Iterable<ModelData> models) {
		//count totals
		int indexTotal = 0;
		int vertexTotal = 0;
		for(ModelData model : models) {
			if(model.positions().length / POSITION_SIZE != model.textureCoords().length / TEXTURE_COORD_SIZE)
				throw new IllegalArgumentException("vertex count mismatch");
			indexTotal += model.indexCount();
			vertexTotal += model.positions().length / POSITION_SIZE;
		}

		//TODO remove repeat vertices (same position and texture coord) and readjust indices?
		//initialize temporary buffers
		FloatBuffer positionData = BufferUtils.createFloatBuffer(vertexTotal * POSITION_SIZE);
		FloatBuffer textureCoordsData = BufferUtils.createFloatBuffer(vertexTotal * TEXTURE_COORD_SIZE);
		IntBuffer indexData = BufferUtils.createIntBuffer(indexTotal);

		for(ModelData model : models) {
			int vertexOffset = positionData.position() / POSITION_SIZE;
			positionData.put(model.positions());
			textureCoordsData.put(model.textureCoords());

			int indexOffset = indexData.position();
			for(int index : model.indices()) {
				indexData.put(vertexOffset + index); //adjust index to be absolute instead of relative to model
			}
			model.out().set(new ModelAttributes(indexOffset, model.indexCount(), model.width(), model.height()));
		}

		//TODO interleave positions and texture coordinates, and make it possible to add more components with more dynamic placement
		int positionsID = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, positionsID);
		glBufferStorage(GL_ARRAY_BUFFER, positionData.flip(), 0);

		int textureCoordsID = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, textureCoordsID);
		glBufferStorage(GL_ARRAY_BUFFER, textureCoordsData.flip(), 0);

		int indicesID = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesID);
		glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, indexData.flip(), 0);

		GraphicsUtility.checkErrors();

		return new ModelBuffers(positionsID, textureCoordsID, indicesID);
	}

	public static int defineVAO(ModelBuffers data, int attributePositions, int attributeTextureCoords) {
		int vao = glGenVertexArrays();
		glBindVertexArray(vao);
		//TODO interleave positions and texture coordinates, and make it possible to add more components with more dynamic placement
		//can also use DSA functions glBindVertexBuffer or glVertexArrayVertexBuffers
		glBindBuffer(GL_ARRAY_BUFFER, data.positionsVBO);
		glEnableVertexAttribArray(attributePositions);
		glVertexAttribPointer(attributePositions, POSITION_SIZE, GL_FLOAT, false, 0, 0);
		glBindBuffer(GL_ARRAY_BUFFER, data.textureCoordsVBO);
		glEnableVertexAttribArray(attributeTextureCoords);
		glVertexAttribPointer(attributeTextureCoords, TEXTURE_COORD_SIZE, GL_FLOAT, false, 0, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, data.indicesVBO);
		glBindVertexArray(0);
		GraphicsUtility.checkErrors();
		return vao;
	}

	public static int defineVAOPositionsOnly(ModelBuffers data, int attributePositions) {
		int vao = glGenVertexArrays();
		glBindVertexArray(vao);
		//TODO interleave positions and texture coordinates, and make it possible to add more components with more dynamic placement
		glBindBuffer(GL_ARRAY_BUFFER, data.positionsVBO);
		glEnableVertexAttribArray(attributePositions);
		glVertexAttribPointer(attributePositions, POSITION_SIZE, GL_FLOAT, false, 0, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, data.indicesVBO);
		glBindVertexArray(0);
		GraphicsUtility.checkErrors();
		return vao;
	}
}
