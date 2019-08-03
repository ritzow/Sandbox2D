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
//
//	private final float[] cameraMatrix = {
//			1, 0, 0, 0,
//			0, 1, 0, 0,
//			0, 0, 0, 0,
//			0, 0, 0, 1,
//	};

	private final float[] viewMatrix = {
			0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};
	
//	private final float[] transformationMatrix = {
//			1, 0, 0, 0,
//			0, 1, 0, 0,
//			0, 0, 0, 0,
//			0, 0, 0, 1,
//	};
	
//	private static final int
//		SCALE_X = 0,
//		SHEAR_X = 1,
//		TRANS_X = 3,
//		SHEAR_Y = 4,
//		SCALE_Y = 5,
//		TRANS_Y = 7;
	
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
		this.modelProperties = new HashMap<>();
		this.renderQueue = new ArrayDeque<>();
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

//	public void loadViewMatrix2(Camera camera, int framebufferWidth, int framebufferHeight) {
//		computeAspectMatrix(framebufferWidth, framebufferHeight);
//		computeCameraMatrix(camera);
//		multiply(aspectMatrix, cameraMatrix, viewMatrix);
//		setMatrix(uniform_view, viewMatrix);
//	}
	
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

//	//TODO figure out the weird vertical squashing from aspect ratio
//	private void computeAspectMatrix(int framebufferWidth, int framebufferHeight) {
//		float ratio = framebufferWidth/(float)framebufferHeight;
//		aspectMatrix[SCALE_X] = 1/ratio;
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
//	}

//	private void computeCameraMatrix(Camera camera) {
//		cameraMatrix[SCALE_X] = camera.getZoom();
//		cameraMatrix[TRANS_X] = -camera.getPositionX() * camera.getZoom();
//		cameraMatrix[SCALE_Y] = camera.getZoom();
//		cameraMatrix[TRANS_Y] = -camera.getPositionY() * camera.getZoom();
//	}

//	//matrix operations on 4x4 1-dimensional arrays
//	private static void multiply(float[] a, float[] b, float[] destination) {
//		for(int i = 0; i <= 12; i += 4) {
//			for(int j = 0; j < 4; j++) {
//				TODO use Math.fma to speed this up if needed
//				destination[i + j] = 
//				     a[i]     * b[j]
//				   + a[i + 1] * b[j + 4]
//				   + a[i + 2] * b[j + 8]
//				   + a[i + 3] * b[j + 12];
//			}
//		}
//	}
	
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
		
		//setupUBO();
		
		int indicesID = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesID);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData, GL_STATIC_DRAW);
		
		glBindVertexArray(0);
		GraphicsUtility.checkErrors();
		
		return vao;
	}
	
//	private int 
//		instanceDataID,
//		instanceDataSize, 
//		transformOffset, 
//		opacityOffset, 
//		transformMatrixStride,
//		transformArrayStride,
//		opacityArrayStride;
	
//	private void loadInstanceData(ByteBuffer dest, float posX, float posY, 
//			float scaleX, float scaleY, float rotation, float opacity) {
//		int start = dest.position();
//		int transformStart = start + transformOffset;
//		float rotX = (float)Math.cos(rotation);
//		float rotY = (float)Math.sin(rotation);
//		//column major
//		dest.position(transformStart)
//			.putFloat(scaleX * rotX)
//			.putFloat(scaleX * -rotY)
//			.putFloat(0)
//			.putFloat(0)
//			.putFloat(scaleY * rotY)
//			.putFloat(scaleY * rotX)
//			.putFloat(0)
//			.putFloat(0)
//			.putFloat(0)
//			.putFloat(0)
//			.putFloat(0)
//			.putFloat(0)
//			.putFloat(posX)
//			.putFloat(posY)
//			.putFloat(0)
//			.putFloat(1)
//			.putFloat(opacity);
//	}
	
	private static void prepInstanceData(FloatBuffer dest, float posX, float posY, 
			float scaleX, float scaleY, float rotation) {
		float rotX = (float)Math.cos(rotation);
		float rotY = (float)Math.sin(rotation);
		//column major
		dest.put(scaleX * rotX)
			.put(scaleX * -rotY)
			.put(0)
			.put(0)
			.put(scaleY * rotY)
			.put(scaleY * rotX)
			.put(0)
			.put(0)
			.put(0)
			.put(0)
			.put(0)
			.put(0)
			.put(posX)
			.put(posY)
			.put(0)
			.put(1);
	}
	
//	private void loadTransformationMatrix(float posX, float posY, float scaleX, float scaleY, float rotation) {
//		float rotX = (float)Math.cos(rotation);
//		float rotY = (float)Math.sin(rotation);
//		transformationMatrix[SCALE_X] = scaleX * rotX;
//		transformationMatrix[SHEAR_X] = scaleY * rotY;
//		transformationMatrix[TRANS_X] = posX;
//		transformationMatrix[SHEAR_Y] = scaleX * -rotY;
//		transformationMatrix[SCALE_Y] = scaleY * rotX;
//		transformationMatrix[TRANS_Y] = posY;
//	}
	
	//can use fixed size array with ubo or variable size SSO using extension to enable in opengl 4.1
//	private void setupUBO() {
//		int transformIndex = glGetUniformIndices(programID, "transform");
//		int opacityIndex = glGetUniformIndices(programID, "opacity");
//		int instanceDataIndex = glGetUniformBlockIndex(programID, "InstanceData");
//		
//		//TODO use glGetActiveUniformBlockiv to get all of these at once.
//		instanceDataSize = glGetActiveUniformBlocki(programID, instanceDataIndex, GL_UNIFORM_BLOCK_DATA_SIZE);
//		transformOffset = glGetActiveUniformsi(programID, transformIndex, GL_UNIFORM_OFFSET);
//		opacityOffset = glGetActiveUniformsi(programID, opacityIndex, GL_UNIFORM_OFFSET);
//		transformMatrixStride = glGetActiveUniformsi(programID, transformIndex, GL_UNIFORM_MATRIX_STRIDE);
//		transformArrayStride = glGetActiveUniformsi(programID, transformIndex, GL_UNIFORM_ARRAY_STRIDE);
//		opacityArrayStride = glGetActiveUniformsi(programID, opacityIndex, GL_UNIFORM_ARRAY_STRIDE);
//		
//		instanceDataID = glGenBuffers();
//		glBindBuffer(GL_UNIFORM_BUFFER, instanceDataID);
//		glBindBufferBase(GL_UNIFORM_BUFFER, instanceDataIndex, instanceDataID);
//		glBufferData(GL_UNIFORM_BUFFER, instanceDataSize, GL_STREAM_DRAW);
//
//		System.out.println("size: " + instanceDataSize + 
//				" transform " + transformIndex + " opacity " + opacityIndex + 
//				", transform offset: " + transformOffset + 
//				", opacity offset: " + opacityOffset + 
//				" transform matrix stride " + transformMatrixStride);
//	}
	
	public void queueRender(int modelID, float opacity,
				float posX, float posY, float scaleX, float scaleY,
				float rotation) {
		renderQueue.add(new RenderInstance(modelID, opacity, posX, posY, scaleX, scaleY, rotation));
	}
	
	//TODO maybe I should change the queue to actually buffer indices, etc. directly instead of RenderInstances
	public void queueRender(RenderInstance model) {
		renderQueue.add(Objects.requireNonNull(model));
	}
	
	public void queueRender(Collection<RenderInstance> models) {
		renderQueue.addAll(models);
	}
	
	private float lastOpacity = 0;
	
	//TODO something is causing a crash
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
		
		/*
		try(MemoryStack stack = MemoryStack.stackPush()) {
			if(stack.getPointer() < renderQueue.size() * Integer.BYTES * Pointer.POINTER_SIZE * instanceDataSize) {
				throw new RuntimeException("not enough space to allocate multidraw buffers on stack: "
						+ Utility.formatSize(stack.getPointer()));	
			}
			
			IntBuffer counts = stack.mallocInt(renderQueue.size());
			PointerBuffer offsets = stack.mallocPointer(renderQueue.size());
			
			//size of a single uniform times mdoels.size TODO do I need to allocate with alignment?
			ByteBuffer instanceData = stack.malloc(instanceDataSize * renderQueue.size());
			
			while(!renderQueue.isEmpty()) {
				RenderInstance model = renderQueue.poll();
				ModelAttributes data = modelProperties.get(model.modelID);
				offsets.put(data.indexOffset * Integer.BYTES);
				counts.put(data.indexCount);
				
				//temporary
				//setFloat(uniform_opacity, model.opacity);
				//loadTransformationMatrix(model.posX, model.posY, model.scaleX, model.scaleY, model.rotation);	
				//setMatrix(uniform_transform, transformationMatrix);
				
				//TODO put transform matrices into in vbo/uniform aligned based on earlier queried offsets/alignment
				loadInstanceData(instanceData.position(), model.posX, model.posY, 
						model.scaleX, model.scaleY, model.rotation, model.opacity);
				
			}
			glBindBuffer(GL_UNIFORM_BUFFER, instanceDataID);
			glBufferData(GL_UNIFORM_BUFFER, instanceData.flip(), GL_STREAM_DRAW);
			glBindVertexArray(vaoID);
			glMultiDrawElements(GL_TRIANGLES, counts.flip(), GL_UNSIGNED_INT, offsets.flip());
		}
		GraphicsUtility.checkErrors();
		*/
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