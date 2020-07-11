package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL46C.*;

public final class ModelRenderProgramEnhanced extends ModelRenderProgramBase {
	private static final int INDIRECT_STRUCT_SIZE = 5 * Integer.BYTES;
	private static final int INSTANCE_STRUCT_SIZE = (16 + 1 + 3) * Float.BYTES /* padding for opacity */;
	private static final int OFFSET_SIZE = Integer.BYTES * 4;
	private static final int MAX_RENDER_COUNT = 300;

	/** Used to store all per-frame rendering data for a fixed maximum number of model renders **/
	private final int indirectVBO, drawDataVBO;
	private final int offsetsSize;
	private final ByteBuffer drawBuffer;
	private final ByteBuffer indirectBuffer;
	private Model model;
	private int renderIndex, commandIndex, instanceCount;

	public ModelRenderProgramEnhanced(Shader vertexShader, Shader fragmentShader, int textureAtlas, ModelData... models) {
		super(vertexShader, fragmentShader, textureAtlas, models);
		//https://learnopengl.com/Advanced-OpenGL/Advanced-GLSL
		IntBuffer params = BufferUtils.createIntBuffer(2);
		IntBuffer props = BufferUtils.createIntBuffer(2)
		  .put(GL_BUFFER_BINDING).put(GL_BUFFER_DATA_SIZE).flip();
		int instanceDataIndex = glGetProgramResourceIndex(programID, GL_UNIFORM_BLOCK, "instance_data");
		glGetProgramResourceiv(programID, GL_UNIFORM_BLOCK, instanceDataIndex, props, null, params);
		int instanceDataBinding = params.get(0);
		int bufferSize = params.get(1);
		indirectVBO = glGenBuffers();
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVBO);
		glBufferStorage(GL_DRAW_INDIRECT_BUFFER, MAX_RENDER_COUNT * INDIRECT_STRUCT_SIZE, GL_DYNAMIC_STORAGE_BIT);
		this.indirectBuffer = BufferUtils.createByteBuffer(MAX_RENDER_COUNT * INDIRECT_STRUCT_SIZE);
		drawDataVBO = glGenBuffers();
		glBindBuffer(GL_UNIFORM_BUFFER, drawDataVBO);
		glBufferStorage(GL_UNIFORM_BUFFER, bufferSize, GL_DYNAMIC_STORAGE_BIT);
		//connect the UBO to the shader instance_data block by binding it to binding point specified in the shader
		glBindBufferBase(GL_UNIFORM_BUFFER, instanceDataBinding, drawDataVBO);
		this.drawBuffer = BufferUtils.createByteBuffer(bufferSize);
		this.offsetsSize = MAX_RENDER_COUNT * OFFSET_SIZE;
		GraphicsUtility.checkErrors();
	}

	@Override
	public void queueRender(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		Objects.requireNonNull(model);
		putRenderInstance(drawBuffer.position(offsetsSize + (renderIndex + instanceCount) * INSTANCE_STRUCT_SIZE),
			opacity, posX, posY, scaleX, scaleY, rotation);
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
		drawBuffer.position(commandIndex * OFFSET_SIZE).putInt(renderIndex);
		ModelAttributes properties = modelProperties.get(model);
		indirectBuffer
			.putInt(properties.indexCount())
			.putInt(instanceCount)
			.putInt(properties.indexOffset())
			.putInt(0) //baseVertex is always 0
			.putInt(0); //baseInstance is always 0
		commandIndex++;
		renderIndex += instanceCount;
	}

	private void render() {
		//upload all the data to the GPU
		glBindVertexArray(vaoID);
		glBindBuffer(GL_UNIFORM_BUFFER, drawDataVBO);
		glBufferSubData(GL_UNIFORM_BUFFER, 0, drawBuffer.position(0).limit(offsetsSize + renderIndex * INSTANCE_STRUCT_SIZE));
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVBO);
		glBufferSubData(GL_DRAW_INDIRECT_BUFFER, 0, indirectBuffer.flip());
		glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, 0, commandIndex, 0);
		drawBuffer.clear();
		indirectBuffer.clear();
		renderIndex = 0;
		commandIndex = 0;
		instanceCount = 0;
	}

	@Override
	public void flush() {
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

	private static void putRenderInstance(ByteBuffer drawBuffer,
		float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		float rotX = (float)Math.cos(rotation);
		float rotY = (float)Math.sin(rotation);
		drawBuffer
			.putFloat(opacity).putFloat(0).putFloat(0).putFloat(0)							 //opacity and padding
			.putFloat(scaleX * rotX)	.putFloat(scaleX * -rotY)	.putFloat(0).putFloat(0) //column major matrix
			.putFloat(scaleY * rotY)	.putFloat(scaleY * rotX)	.putFloat(0).putFloat(0)
			.putFloat(0)				.putFloat(0)				.putFloat(0).putFloat(0)
			.putFloat(posX)				.putFloat(posY)				.putFloat(0).putFloat(1);
	}

	private static void putRenderInstance(ByteBuffer drawBuffer, RenderInstance render) {
		float scaleX = render.scaleX();
		float scaleY = render.scaleY();
		float rotation = render.rotation();
		float rotX = (float)Math.cos(rotation);
		float rotY = (float)Math.sin(rotation);
		drawBuffer
			.putFloat(render.opacity())
			.putFloat(0)
			.putFloat(0)
			.putFloat(0)
			.putFloat(scaleX * rotX)	.putFloat(scaleX * -rotY)	.putFloat(0).putFloat(0) //column major
			.putFloat(scaleY * rotY)	.putFloat(scaleY * rotX)	.putFloat(0).putFloat(0)
			.putFloat(0)				.putFloat(0)				.putFloat(0).putFloat(0)
			.putFloat(render.posX())	.putFloat(render.posY())	.putFloat(0).putFloat(1);
	}
}