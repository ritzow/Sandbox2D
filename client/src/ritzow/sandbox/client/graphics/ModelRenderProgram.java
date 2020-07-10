package ritzow.sandbox.client.graphics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import ritzow.sandbox.client.data.StandardClientOptions;

import static org.lwjgl.opengl.GL46C.*;

public class ModelRenderProgram extends ShaderProgram {

	public static final record RenderInstance(Model model, float opacity,
		float posX, float posY, float scaleX, float scaleY, float rotation) {}
	protected static record ModelAttributes(int indexOffset, int indexCount) {}

	protected static final int POSITION_SIZE = 2, TEXTURE_COORD_SIZE = 2;
	protected static final int ATLAS_TEXTURE_UNIT = GL_TEXTURE0;

	private final float[] aspectMatrix = {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};

	private final float[] viewMatrix = {
			0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};

	protected final int
		uniform_transform,
		uniform_opacity,
		uniform_view,
		attribute_position,
		attribute_textureCoord;

	protected final int vaoID, atlasTexture;
	protected final Queue<RenderInstance> renderQueue;
	protected final Map<Model, ModelAttributes> modelProperties;

	public static ModelRenderProgram create(Shader vertexShader, Shader fragmentShader,
			int textureAtlas, ModelData... models) {
		return StandardClientOptions.USE_OPENGL_4_6 ?
			new ModelRenderProgramEnhanced(vertexShader, fragmentShader, textureAtlas, models) :
			new ModelRenderProgram(vertexShader, fragmentShader, textureAtlas, models);
	}

	protected ModelRenderProgram(Shader vertexShader, Shader fragmentShader,
		int textureAtlas, ModelData... models) {
		super(vertexShader, fragmentShader);
		this.uniform_transform = getUniformLocation("transform");
		this.uniform_opacity = getUniformLocation("opacity");
		this.attribute_position = getAttributeLocation("position");
		this.attribute_textureCoord = getAttributeLocation("textureCoord");
		this.uniform_view = getUniformLocation("view");
		setInteger(getUniformLocation("atlasSampler"), ATLAS_TEXTURE_UNIT);
		this.atlasTexture = textureAtlas;
		this.modelProperties = new HashMap<Model, ModelAttributes>();
		this.renderQueue = new ArrayDeque<RenderInstance>();
		this.vaoID = register(models);
	}

	public static record ModelData(Model model, float[] positions, float[] textureCoords, int[] indices) {
		public int indexCount() {
			return indices.length;
		}
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

	//TODO I still want to use glMultiDrawElements to improve efficiency, need to just draw as many models as are allowed by max array size in glsl

	/**
	 * Renders a model with the specified properties
	 * @param model the model to render
	 * @param opacity the opacity of the model
	 * @param posX the horizontal position to render the model
	 * @param posY the vertical position to render the model
	 * @param scaleX the horizontal stretch scale of the model
	 * @param scaleY the vertical stretch scale of the model
	 * @param rotation the rotation, in radians, of the model
	 */
	public void render(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		ModelAttributes attribs = modelProperties.get(model);
		if(attribs == null)
			throw new IllegalArgumentException("no data exists for model " + model);
		try(MemoryStack stack = MemoryStack.stackPush()) {
			FloatBuffer instanceData = stack.mallocFloat(16);
			prepInstanceData(instanceData, posX, posY, scaleX, scaleY, rotation);
			glUniformMatrix4fv(uniform_transform, false, instanceData.flip());
			if(lastOpacity != opacity) {
				setFloat(uniform_opacity, opacity);
				lastOpacity = opacity;
			}
			glBindVertexArray(vaoID);
			glDrawElements(GL_TRIANGLES, attribs.indexCount, GL_UNSIGNED_INT, attribs.indexOffset * Integer.BYTES);
		}
	}

	public void render(Graphics g, float posX, float posY) {
		render(g.getModel(), g.getOpacity(), posX, posY, g.getScaleX(), g.getScaleY(), g.getRotation());
	}

	public void loadViewMatrixStandard(int framebufferWidth, int framebufferHeight) {
		aspectMatrix[0] = framebufferHeight/(float)framebufferWidth;
		setMatrix(uniform_view, aspectMatrix);
	}

	public void loadViewMatrix(Camera camera, int framebufferWidth, int framebufferHeight) {
		float ratio = framebufferHeight/(float)framebufferWidth;
		float zoom = camera.getZoom();
		float ratioZoom = ratio * zoom;
		viewMatrix[0] = ratioZoom;
		viewMatrix[2] = ratio;
		viewMatrix[3] = -ratioZoom * camera.getPositionX();
		viewMatrix[5] = zoom;
		viewMatrix[7] = -zoom * camera.getPositionY();
		setMatrix(uniform_view, viewMatrix);
	}

	private int register(ModelData[] models) {
		//count totals
		int indexTotal = 0;
		int vertexTotal = 0;
		for(ModelData model : models) {
			if(model.positions.length/POSITION_SIZE != model.textureCoords.length/TEXTURE_COORD_SIZE)
				throw new IllegalArgumentException("vertex count mismatch");
			indexTotal += model.indexCount();
			vertexTotal += model.positions.length/POSITION_SIZE;
		}

		//TODO remove repeat vertices (same position and texture coord) and readjust indices?
		//initialize temporary buffers
		FloatBuffer positionData = BufferUtils.createFloatBuffer(vertexTotal * POSITION_SIZE);
		FloatBuffer textureCoordsData = BufferUtils.createFloatBuffer(vertexTotal * TEXTURE_COORD_SIZE);
		IntBuffer indexData = BufferUtils.createIntBuffer(indexTotal);

		for(ModelData model : models) {
			int vertexOffset = positionData.position()/POSITION_SIZE;
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

	protected static void prepInstanceData(FloatBuffer dest, float posX, float posY,
								 float scaleX, float scaleY, float rotation) {
		float rotX = (float)Math.cos(rotation);
		float rotY = (float)Math.sin(rotation);
		dest.put(scaleX * rotX)	.put(scaleX * -rotY)	.put(0).put(0) //column major
			.put(scaleY * rotY)	.put(scaleY * rotX)		.put(0).put(0)
			.put(0)				.put(0)					.put(0).put(0)
			.put(posX)			.put(posY)				.put(0).put(1);
	}

	public void queueRender(Model model, float opacity,
				float posX, float posY, float scaleX, float scaleY,
				float rotation) {
		renderQueue.add(new RenderInstance(model, opacity, posX, posY, scaleX, scaleY, rotation));
	}

	//TODO maybe I should change the queue to actually buffer indices, etc. directly instead of RenderInstances
	public void renderQueue(RenderInstance model) {
		renderQueue.add(Objects.requireNonNull(model));
	}

	public void renderQueue(Collection<RenderInstance> models) {
		renderQueue.addAll(models);
	}

	private float lastOpacity = 0;

	/** Render queued RenderInstances **/
	public void render() {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			FloatBuffer instanceData = stack.mallocFloat(16);
			glBindVertexArray(vaoID);
			while(!renderQueue.isEmpty()) {
				RenderInstance render = renderQueue.poll();
				ModelAttributes model = modelProperties.get(render.model);
				prepInstanceData(instanceData, render.posX, render.posY, render.scaleX, render.scaleY, render.rotation);
				glUniformMatrix4fv(uniform_transform, false, instanceData.flip());
				if(render.opacity != lastOpacity) {
					setFloat(uniform_opacity, render.opacity);
					lastOpacity = render.opacity;
				}
				glDrawElements(GL_TRIANGLES, model.indexCount, GL_UNSIGNED_INT, model.indexOffset * Integer.BYTES);
			}
		}
	}
}