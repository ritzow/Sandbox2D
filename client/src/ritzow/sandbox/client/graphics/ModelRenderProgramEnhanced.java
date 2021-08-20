package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import org.lwjgl.BufferUtils;
import ritzow.sandbox.client.graphics.ModelStorage.ModelBuffers;
import ritzow.sandbox.client.graphics.ModelStorage.ModelAttributes;

import static org.lwjgl.opengl.GL46C.*;

public final class ModelRenderProgramEnhanced extends ModelRenderProgramBase {
	private static final int INDIRECT_STRUCT_SIZE = 5 * Integer.BYTES;
	private static final int INSTANCE_STRUCT_SIZE = (16 + 1 + 3) * Float.BYTES /* padding for opacity */;
	private static final int OFFSET_SIZE = Integer.BYTES * 4;
	private static final int MAX_RENDER_COUNT = 100;

	/** Used to store all per-frame rendering data for a fixed maximum number of model renders **/
	private final int indirectVBO, drawDataVBO, instanceDataBinding;
	private final int offsetsSize;
	private final ByteBuffer drawBuffer;
	private final ByteBuffer indirectBuffer;
	private Model model;
	private int
		renderIndex,    //total number of model instances queued so far for the next glMultiDrawElementsInstanced call
		commandIndex,   //current instanced draw command, single draw ID
		instanceCount;  //instance count for the current model for the current draw ID

	public ModelRenderProgramEnhanced(Shader vertexShader, Shader fragmentShader, int textureAtlas, ModelBuffers models) {
		super(vertexShader, fragmentShader, textureAtlas, models);
		//https://learnopengl.com/Advanced-OpenGL/Advanced-GLSL
		IntBuffer params = BufferUtils.createIntBuffer(2);
		IntBuffer props = BufferUtils.createIntBuffer(2)
			.put(GL_BUFFER_BINDING).put(GL_BUFFER_DATA_SIZE).flip();
		int instanceDataIndex = glGetProgramResourceIndex(programID, GL_UNIFORM_BLOCK, "instance_data");
		glGetProgramResourceiv(programID, GL_UNIFORM_BLOCK, instanceDataIndex, props, null, params);
		instanceDataBinding = params.get(0);
		int bufferSize = params.get(1);
		indirectVBO = glGenBuffers();
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVBO);
		glBufferStorage(GL_DRAW_INDIRECT_BUFFER, MAX_RENDER_COUNT * INDIRECT_STRUCT_SIZE, GL_DYNAMIC_STORAGE_BIT);
		this.indirectBuffer = BufferUtils.createByteBuffer(MAX_RENDER_COUNT * INDIRECT_STRUCT_SIZE);
		drawDataVBO = glGenBuffers();
		glBindBuffer(GL_UNIFORM_BUFFER, drawDataVBO);
		glBufferStorage(GL_UNIFORM_BUFFER, bufferSize, GL_DYNAMIC_STORAGE_BIT);
		//connect the UBO to the shader instance_data block by binding it to binding point specified in the shader
		this.offsetsSize = MAX_RENDER_COUNT * OFFSET_SIZE;
		this.drawBuffer = BufferUtils.createByteBuffer(bufferSize).position(offsetsSize);
		GraphicsUtility.checkErrors();
	}

	@Override
	public void queueRender(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		queueRender(model, opacity, 1.0f, posX, posY, scaleX, scaleY, rotation);
	}

	@Override
	public void queueRender(Model model, float opacity, float exposure, float posX, float posY, float scaleX, float scaleY, float rotation) {
		Objects.requireNonNull(model);
		putRenderInstance(drawBuffer, opacity, exposure, posX, posY, scaleX, scaleY, rotation);
		if(instanceCount == 0) { //aka this.model == null
			this.model = model;
			instanceCount++;
		} else {
			if(model.equals(this.model)) {
				instanceCount++;
			} else {
				nextCommand(this.model);
				this.model = model;
				instanceCount = 1;
			}

			if(renderIndex + instanceCount == MAX_RENDER_COUNT) {
				nextCommand(this.model);
				render();
			}
		}
	}

	private void nextCommand(Model model) {
		drawBuffer.putInt(commandIndex * OFFSET_SIZE, renderIndex);
		ModelAttributes properties = (ModelAttributes)model;
		indirectBuffer
			.putInt(properties.indexCount())
			.putInt(instanceCount)
			.putInt(properties.indexOffset())
			.putInt(0) //baseVertex is always 0
			.putInt(0); //baseInstance is always 0
		commandIndex++;
		renderIndex += instanceCount;
	}

	@Override
	public void prepare() {
		super.prepare();
		glBindVertexArray(vaoID);
	}

	private void render() {
		//upload all the data to the GPU
		glBindBuffer(GL_UNIFORM_BUFFER, drawDataVBO);
		glBindBufferBase(GL_UNIFORM_BUFFER, instanceDataBinding, drawDataVBO);
		if(renderIndex == MAX_RENDER_COUNT) {
			glBufferSubData(GL_UNIFORM_BUFFER, 0, drawBuffer.limit(offsetsSize + renderIndex * INSTANCE_STRUCT_SIZE).position(0));
		} else {
			glBufferSubData(GL_UNIFORM_BUFFER, 0, drawBuffer.limit(commandIndex * OFFSET_SIZE).position(0));
			glBufferSubData(GL_UNIFORM_BUFFER, offsetsSize, drawBuffer.limit(offsetsSize + renderIndex * INSTANCE_STRUCT_SIZE).position(offsetsSize));
		}
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVBO);
		glBufferSubData(GL_DRAW_INDIRECT_BUFFER, 0, indirectBuffer.flip());
		glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, 0, commandIndex, 0);
		drawBuffer.clear().position(offsetsSize);
		indirectBuffer.clear();
		renderIndex = 0;
		commandIndex = 0;
		instanceCount = 0;
	}

	@Override
	public void flush() {
		//TODO call this inside the view matrix load methods
		//instanceCount will be at least 1 if queueRender didn't call render() immediately prior
		if(instanceCount > 0) {
			nextCommand(this.model);
			render();
		}
	}

	@Override
	public void delete() {
		super.delete();
		glDeleteBuffers(drawDataVBO);
		glDeleteBuffers(indirectVBO);
	}

	//TODO need to make sure values are pixel perfect to prevent one-pixel gaps in blocks
	private static void putRenderInstance(ByteBuffer drawBuffer,
		float opacity, float exposure, float posX, float posY, float scaleX, float scaleY, float rotation) {
		double rotX = Math.cos(rotation);
		double rotY = Math.sin(rotation);
		drawBuffer
			.putFloat(opacity)					.putFloat(exposure)					.putInt(0).putInt(0) //align //opacity and padding
			.putFloat((float)(scaleX * rotX))	.putFloat((float)(scaleX * -rotY))	.putFloat(0).putFloat(0) //column major matrix
			.putFloat((float)(scaleY * rotY))	.putFloat((float)(scaleY * rotX))	.putFloat(0).putFloat(0)
			.putFloat(0)						.putFloat(0)						.putFloat(0).putFloat(0)
			.putFloat(posX)						.putFloat(posY)						.putFloat(0).putFloat(1);
	}
}