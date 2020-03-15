package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL46C.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

public final class ModelRenderProgram extends ShaderProgram {
	
	private static final int POSITION_SIZE = 2, TEXTURE_COORD_SIZE = 2;
	private static final int ATLAS_TEXTURE_UNIT = GL_TEXTURE0;

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
	
	private final int
		uniform_transform,
		uniform_opacity,
		uniform_view,
		attribute_position,
		attribute_textureCoord;

	private final int vaoID, atlasTexture;
	private final Queue<RenderInstance> renderQueue;
	private final Map<Integer, ModelAttributes> modelProperties;
	
	public static ModelRenderProgram create(Shader vertexShader, Shader fragmentShader, int textureAtlas, ModelData... models) {
		return new ModelRenderProgram(vertexShader, fragmentShader, textureAtlas, models);
	}

	private ModelRenderProgram(Shader vertexShader, Shader fragmentShader, int textureAtlas, ModelData... models) {
		super(vertexShader, fragmentShader);
		this.uniform_transform = getUniformLocation("transform");
		this.uniform_opacity = getUniformLocation("opacity");
		this.attribute_position = getAttributeLocation("position");
		this.attribute_textureCoord = getAttributeLocation("textureCoord");
		this.uniform_view = getUniformLocation("view");
		setInteger(getUniformLocation("atlasSampler"), ATLAS_TEXTURE_UNIT);
		this.atlasTexture = textureAtlas;
		this.modelProperties = new HashMap<Integer, ModelAttributes>();
		this.renderQueue = new ArrayDeque<RenderInstance>();
		this.vaoID = register(models);
	}
	
	public static class ModelData {
		final int modelID;
		final float[] positions;
		final float[] textureCoords;
		final int[] indices;
		
		public ModelData(int modelID, float[] positions, float[] textureCoords, int[] indices) {
			this.modelID = modelID;
			this.positions = positions;
			this.textureCoords = textureCoords;
			this.indices = indices;
		}
		
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
	
	/**
	 * Renders a model with the specified properties
	 * @param modelID the model to render
	 * @param opacity the opacity of the model
	 * @param posX the horizontal position to render the model
	 * @param posY the vertical position to render the model
	 * @param scaleX the horizontal stretch scale of the model
	 * @param scaleY the vertical stretch scale of the model
	 * @param rotation the rotation, in radians, of the model
	 */
	public void render(int modelID, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		ModelAttributes model = modelProperties.get(modelID);
		if(model == null)
			throw new IllegalArgumentException("no data exists for model id " + modelID);
		try(MemoryStack stack = MemoryStack.stackPush()) {
			FloatBuffer instanceData = stack.mallocFloat(16);
			prepInstanceData(instanceData, posX, posY, scaleX, scaleY, rotation);
			glUniformMatrix4fv(uniform_transform, false, instanceData.flip());
			if(lastOpacity != opacity) {
				setFloat(uniform_opacity, opacity);
				lastOpacity = opacity;
			}
			glBindVertexArray(vaoID);
			glDrawElements(GL_TRIANGLES, model.indexCount, GL_UNSIGNED_INT, model.indexOffset * Integer.BYTES);		
		}
	}
	
	public void render(Graphics g, float posX, float posY) {
		render(g.getModelID(), g.getOpacity(), posX, posY, g.getScaleX(), g.getScaleY(), g.getRotation());
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
		
		//TODO remove repeat vertices (same position and texture coord)?
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
			
			modelProperties.put(model.modelID, new ModelAttributes(indexOffset, model.indexCount()));
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
	
	private static void prepInstanceData(FloatBuffer dest, float posX, float posY, 
			float scaleX, float scaleY, float rotation) {
		float rotX = (float)Math.cos(rotation);
		float rotY = (float)Math.sin(rotation);
		dest.put(scaleX * rotX)	.put(scaleX * -rotY)	.put(0).put(0) //column major
			.put(scaleY * rotY)	.put(scaleY * rotX)		.put(0).put(0)
			.put(0)				.put(0)					.put(0).put(0)
			.put(posX)			.put(posY)				.put(0).put(1);
	}
	
	public void queueRender(int modelID, float opacity,
				float posX, float posY, float scaleX, float scaleY,
				float rotation) {
		renderQueue.add(new RenderInstance(modelID, opacity, posX, posY, scaleX, scaleY, rotation));
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
		//glMultiDrawElementsIndirect works even better but requires OpenGL 4.3
		//gl_DrawID can be used from glsl for this
		//https://www.khronos.org/opengl/wiki/Built-in_Variable_(GLSL)#Vertex_shader_inputs
		
		try(MemoryStack stack = MemoryStack.stackPush()) {
			FloatBuffer instanceData = stack.mallocFloat(16);
			glBindVertexArray(vaoID);
			while(!renderQueue.isEmpty()) {
				RenderInstance render = renderQueue.poll();
				ModelAttributes model = modelProperties.get(render.modelID);
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
	
	public static final class RenderInstance {
		final int modelID;
		final float opacity;
		final float posX;
		final float posY;
		final float scaleX;
		final float scaleY;
		final float rotation;
		
		public RenderInstance(int modelID, float opacity,
				float posX, float posY, float scaleX, float scaleY,
				float rotation) {
			this.modelID = modelID;
			this.opacity = opacity;
			this.posX = posX;
			this.posY = posY;
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			this.rotation = rotation;
		}
	}
	
	private static class ModelAttributes {
		final int indexOffset;
		final int indexCount;
		
		ModelAttributes(int indexOffset, int indexCount) {
			this.indexOffset = indexOffset;
			this.indexCount = indexCount;
		}
	}
}