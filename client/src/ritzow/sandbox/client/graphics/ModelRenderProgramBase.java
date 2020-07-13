package ritzow.sandbox.client.graphics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL14C.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;
import static org.lwjgl.opengl.GL40C.glBlendEquationi;
import static org.lwjgl.opengl.GL40C.glBlendFunci;

public abstract class ModelRenderProgramBase extends ShaderProgram implements ModelRenderer {
	protected static final int POSITION_SIZE = 2;
	protected static final int TEXTURE_COORD_SIZE = 2;
	protected static final int ATLAS_TEXTURE_UNIT = GL_TEXTURE0;
	protected final float[] aspectMatrix = {
		1, 0, 0, 0,
		0, 1, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, 1,
	};
	protected final float[] viewMatrix = {
		0, 0, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, 1,
	};

	protected final int uniform_opacity, uniform_view, attribute_position, attribute_textureCoord, vaoID, atlasTexture;
	protected final Map<Model, ModelAttributes> modelProperties;

	public ModelRenderProgramBase(Shader vertex, Shader fragment, int textureAtlas, ModelData... models) {
		super(vertex, fragment);
		this.uniform_opacity = getUniformLocation("opacity");
		this.uniform_view = getUniformLocation("view");
		this.attribute_position = getAttributeLocation("position");
		this.attribute_textureCoord = getAttributeLocation("textureCoord");
		this.atlasTexture = textureAtlas;
		this.modelProperties = new HashMap<Model, ModelAttributes>();
		this.vaoID = register(models);
	}

	@Override
	public void setCurrent() {
		glUseProgram(programID);
		glActiveTexture(ATLAS_TEXTURE_UNIT);
		glBindTexture(GL_TEXTURE_2D, atlasTexture);
		//set the blending mode to allow transparency
		glBlendFunci(RenderManager.MAIN_DRAW_BUFFER_INDEX, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glBlendEquationi(RenderManager.MAIN_DRAW_BUFFER_INDEX, GL_FUNC_ADD);
	}

	protected int register(ModelData[] models) {
		//count totals
		int indexTotal = 0;
		int vertexTotal = 0;
		for(ModelData model : models) {
			if(model.positions.length / POSITION_SIZE != model.textureCoords.length / TEXTURE_COORD_SIZE)
				throw new IllegalArgumentException("vertex count mismatch");
			indexTotal += model.indexCount();
			vertexTotal += model.positions.length / POSITION_SIZE;
		}

		//TODO remove repeat vertices (same position and texture coord) and readjust indices?
		//initialize temporary buffers
		FloatBuffer positionData = BufferUtils.createFloatBuffer(vertexTotal * POSITION_SIZE);
		FloatBuffer textureCoordsData = BufferUtils.createFloatBuffer(vertexTotal * TEXTURE_COORD_SIZE);
		IntBuffer indexData = BufferUtils.createIntBuffer(indexTotal);

		for(ModelData model : models) {
			int vertexOffset = positionData.position() / POSITION_SIZE;
			positionData.put(model.positions);
			textureCoordsData.put(model.textureCoords);

			int indexOffset = indexData.position();
			for(int index : model.indices) {
				indexData.put(vertexOffset + index); //adjust index to be absolute instead of relative to model
			}

			modelProperties.put(model.model, new ModelAttributes(indexOffset, model.indexCount()));
		}
		positionData.flip();
		textureCoordsData.flip();
		indexData.flip();

		int vao = glGenVertexArrays();
		glBindVertexArray(vao);

		//TODO interleave positions and texture coordinates, and make it possible to add more components
		int positionsID = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, positionsID);
		glBufferData(GL_ARRAY_BUFFER, positionData, GL_STATIC_DRAW);
		glEnableVertexAttribArray(attribute_position);
		glVertexAttribPointer(attribute_position, POSITION_SIZE, GL_FLOAT, false, 0, 0);

		int textureCoordsID = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, textureCoordsID);
		glBufferData(GL_ARRAY_BUFFER, textureCoordsData, GL_STATIC_DRAW);
		glEnableVertexAttribArray(attribute_textureCoord);
		glVertexAttribPointer(attribute_textureCoord, TEXTURE_COORD_SIZE, GL_FLOAT, false, 0, 0);

		int indicesID = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesID);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData, GL_STATIC_DRAW);

		glBindVertexArray(0);
		GraphicsUtility.checkErrors();

		return vao;
	}

	@Override
	public void loadViewMatrixStandard(int framebufferWidth, int framebufferHeight) {
		aspectMatrix[0] = framebufferHeight/(float)framebufferWidth;
		setMatrix(uniform_view, aspectMatrix);
	}

	@Override
	public void loadViewMatrix(Camera camera, int framebufferWidth, int framebufferHeight) {
		double ratio = (double)framebufferHeight/framebufferWidth;
		float zoom = camera.getZoom();
		double ratioZoom = ratio * zoom;
		viewMatrix[0] = (float)ratioZoom;
		viewMatrix[2] = (float)ratio;
		viewMatrix[3] = (float)(-ratioZoom * camera.getPositionX());
		viewMatrix[5] = zoom;
		viewMatrix[7] = -zoom * camera.getPositionY();
		setMatrix(uniform_view, viewMatrix);
	}

	public static final record RenderInstance(
		Model model,
		float opacity,
		float posX,
		float posY,
		float scaleX,
		float scaleY,
		float rotation) {}

	protected static record ModelAttributes(
		int indexOffset,
		int indexCount) {}

	public static record ModelData(
		Model model,
		float[] positions,
		float[] textureCoords,
		int[] indices) {
		public int indexCount() {
			return indices.length;
		}
	}
}
