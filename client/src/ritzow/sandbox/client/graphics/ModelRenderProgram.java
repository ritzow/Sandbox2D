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
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public final class ModelRenderProgram extends ShaderProgram {
	
	private static final int POSITION_SIZE = 2, TEXTURE_COORD_SIZE = 2;

	private static final int
		SCALE_X = 0,
		SCALE_Y = 5,
		SHEAR_X = 1,
		SHEAR_Y = 4,
		TRANS_X = 3,
		TRANS_Y = 7;

//	private static final float[] identityMatrix = {
//			1, 0, 0, 0,
//			0, 1, 0, 0,
//			0, 0, 0, 0,
//			0, 0, 0, 1,
//	};

	private final float[] aspectMatrix = {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};

	private final float[] cameraMatrix = {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};

	private final float[] viewMatrix = {
			0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 0,
	};

	private final float[] transformationMatrix = {
			1, 0, 0, 0,
			0, 1, 0, 0,
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
		this.attribute_position = getAttributeLocation("position");
		this.attribute_textureCoord = getAttributeLocation("textureCoord");
		this.uniform_transform = getUniformLocation("transform");
		this.uniform_opacity = getUniformLocation("opacity");
		this.uniform_view = getUniformLocation("view");
		setInteger(getUniformLocation("textureSampler"), 0);
		this.atlasTexture = textureAtlas;
		this.modelProperties = new HashMap<>();
		this.renderQueue = new ArrayDeque<>();
		this.vaoID = register(models);
	}

	/** Sets the opacity that will be used when the program renders models **/
	public void loadOpacity(float opacity) {
		setFloat(uniform_opacity, opacity);
	}

	/** Must be called before anything is rendered, only required for render methods **/
	@Override
	public void setCurrent() {
		super.setCurrent();
		setCurrentTexture(0, atlasTexture);
	}

	//TODO I should be able to render every model with a single draw call
	//by uploading all transform data to an array accessed in the shader
	//will require restructuring of VAOs (possibly use only one large one)
	//Instancing: same mesh with different uniforms
	//https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glDrawElementsInstanced.xhtml
	//https://stackoverflow.com/questions/21539234/how-to-do-instancing-the-right-way-in-opengl
	
	//use instanced rendering:
	//or indirect rendering? https://www.khronos.org/opengl/wiki/Vertex_Rendering#Indirect_rendering
	//transform matrices per instance
	//Basically: you need to use one of the “Instanced” 
	//drawing functions to draw multiple instances. 
	//Then you can use glVertexAttribDivisor() to indicate 
	//that a particular attribute is per-instance (or per N instances)
	//rather than per-vertex, i.e. the same element from the attribute 
	//array will be used for all vertices within an instance.
	
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
	public void render(int modelID, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		ModelAttributes model = modelProperties.get(modelID);
		if(model == null)
			throw new IllegalArgumentException("no data exists for model id " + modelID);
		loadOpacity(opacity);
		loadTransformationMatrix(posX, posY, scaleX, scaleY, rotation);
		glBindVertexArray(vaoID);
		glDrawElements(GL_TRIANGLES, model.indexCount, GL_UNSIGNED_INT, model.indexOffset * Integer.BYTES);
	}

	public void render(Graphics g, float posX, float posY) {
		render(g.getModelID(), g.getOpacity(), posX, posY, g.getScaleX(), g.getScaleY(), g.getRotation());
	}

	public void render(Graphics g, float posX, float posY, float scaleX, float scaleY, float rotation, float opacity) {
		render(g.getModelID(), g.getOpacity() * opacity, posX, posY, g.getScaleX() * scaleX, g.getScaleY() * scaleY, g.getRotation() + rotation);
	}

	public void loadTransformationMatrix(float posX, float posY, float scaleX, float scaleY, float rotation) {
		transformationMatrix[SCALE_X] = scaleX * (float) Math.cos(rotation);
		transformationMatrix[SHEAR_X] = scaleY * (float) Math.sin(rotation);
		transformationMatrix[TRANS_X] = posX;
		transformationMatrix[SHEAR_Y] = scaleX * (float) -Math.sin(rotation);
		transformationMatrix[SCALE_Y] = scaleY * (float) Math.cos(rotation);
		transformationMatrix[TRANS_Y] = posY;
		setMatrix(uniform_transform, transformationMatrix);
	}

	public void loadViewMatrixStandard(int framebufferWidth, int framebufferHeight) {
		computeAspectMatrix(framebufferWidth, framebufferHeight);
		setMatrix(uniform_view, aspectMatrix);
	}

	public void loadViewMatrix(Camera camera, int framebufferWidth, int framebufferHeight) {
		computeAspectMatrix(framebufferWidth, framebufferHeight);
		computeCameraMatrix(camera);
		multiply(aspectMatrix, cameraMatrix, viewMatrix);
		setMatrix(uniform_view, viewMatrix);
	}

	//TODO figure out the weird vertical squashing from aspect ratio
	private void computeAspectMatrix(int framebufferWidth, int framebufferHeight) {
		float ratio = framebufferWidth/(float)framebufferHeight;
		aspectMatrix[SCALE_X] = 1/ratio;
		//float scaleY = 1/ratio;
		//float scaleX = scaleY/ratio;
//		aspectMatrix[SCALE_X] = scaleX;
//		aspectMatrix[SCALE_Y] = scaleY;

//		if(framebufferWidth > framebufferHeight) {
//			aspectMatrix[SCALE_X] = 1;
//			aspectMatrix[SCALE_Y] = ratio;
//		} else {
//			aspectMatrix[SCALE_X] = 1/ratio;
//			aspectMatrix[SCALE_Y] = 1;
//		}
	}

	private void computeCameraMatrix(Camera camera) {
		cameraMatrix[SCALE_X] = camera.getZoom();
		cameraMatrix[TRANS_X] = -camera.getPositionX() * camera.getZoom();
		cameraMatrix[SCALE_Y] = camera.getZoom();
		cameraMatrix[TRANS_Y] = -camera.getPositionY() * camera.getZoom();
	}

	//matrix operations on 4x4 1-dimensional arrays
	private static void multiply(float[] a, float[] b, float[] destination) {
		for(int i = 0; i <= 12; i += 4) {
			for(int j = 0; j < 4; j++) {
				destination[i + j] = a[i] * b[j]
								   + a[i + 1] * b[j + 4]
								   + a[i + 2] * b[j + 8]
								   + a[i + 3] * b[j + 12];
			}
		}
	}
	
	private int register(ModelData[] models) {
		//count totals
		int indexTotal = 0;
		int vertexTotal = 0;
		for(ModelData model : models) {
			if(model.positions.length/POSITION_SIZE != model.textureCoords.length/TEXTURE_COORD_SIZE)
				throw new IllegalArgumentException("vertex count mismatch");
			indexTotal += model.indexCount();
			vertexTotal += model.vertexCount();
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
		

		int instanceDataIndex = glGetUniformBlockIndex(programID, "InstanceData");
		System.out.println(instanceDataIndex);
		
		int instanceDataID = glGenBuffers();
		glBindBuffer(GL_UNIFORM_BUFFER, instanceDataID);
		glBufferData(GL_UNIFORM_BUFFER, 0, GL_STREAM_DRAW);
		
//		glBindBufferBase(GL_UNIFORM_BUFFER, instanceDataIndex, instanceDataID);
		
//		try(MemoryStack stack = MemoryStack.stackPush()) {
//			IntBuffer buffer = stack.mallocInt(1);
//			glGetActiveUniformBlockiv(programID, instanceDataIndex, GL_UNIFORM_BLOCK_DATA_SIZE, buffer);
//			instanceDataSize = buffer.get(0);
//			System.out.println("InstanceData size: " + buffer);
//			glGetActiveUniformBlockiv(programID, instanceDataIndex, GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS, buffer);
//			IntBuffer indices = stack.mallocInt(buffer.get(0));
//			glGetActiveUniformBlockiv(programID, instanceDataIndex, GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES, indices);
//		}
		
		int indicesID = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesID);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData, GL_STATIC_DRAW);
		
		glBindVertexArray(0);
		GraphicsUtility.checkErrors();
		
		return vao;
	}
	
	//TODO maybe I should change the queue to actually buffer indices, etc.
	public void queueRender(RenderInstance model) {
		renderQueue.add(Objects.requireNonNull(model));
	}
	
	public void queueRender(Collection<RenderInstance> models) {
		renderQueue.addAll(models);
	}
	
	//TODO something is causing a crash
	/** Render queued RenderInstances **/
	public void render() {
		//glMultiDrawElementsIndirect works even better but requires OpenGL 4.3
		//gl_DrawID can be used from glsl for this
		//https://www.khronos.org/opengl/wiki/Built-in_Variable_(GLSL)#Vertex_shader_inputs
		try(MemoryStack stack = MemoryStack.stackPush()) {
			if(stack.getPointer() < renderQueue.size() * 4)
				throw new RuntimeException("not enough space to allocate multidraw buffers on stack");
			IntBuffer counts = stack.mallocInt(renderQueue.size());
			PointerBuffer offsets = stack.mallocPointer(renderQueue.size());
			//ByteBuffer instanceData = stack.malloc(0 /*size of a single uniform times mdoels.size*/);
			while(!renderQueue.isEmpty()) {
				RenderInstance model = renderQueue.poll();
				ModelAttributes data = modelProperties.get(model.modelID);
				offsets.put(data.indexOffset * Integer.BYTES);
				counts.put(data.indexCount);
				//TODO put transform matrices into in vbo/uniform
				
				loadOpacity(model.opacity);
				loadTransformationMatrix(model.posX, model.posY, model.scaleX, model.scaleY, model.rotation);				
			}
			offsets.flip();
			counts.flip();
			glBindVertexArray(vaoID);
			glMultiDrawElements(GL_TRIANGLES, counts, GL_UNSIGNED_INT, offsets);
		}
		GraphicsUtility.checkErrors();
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
		
		public int vertexCount() {
			return positions.length/POSITION_SIZE;
		}
	}
}