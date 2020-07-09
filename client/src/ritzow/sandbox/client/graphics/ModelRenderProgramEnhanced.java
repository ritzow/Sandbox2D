package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL46C.*;

public final class ModelRenderProgramEnhanced extends ModelRenderProgram {
	private static final int INDIRECT_STRUCT_SIZE = 5 * Integer.BYTES;
	private static final int INSTANCE_STRUCT_SIZE = (16 + 1 + 3) * Float.BYTES /* padding for opacity */;
	private static final int OFFSET_SIZE = Integer.BYTES * 4;
	private static final int MAX_RENDER_COUNT = 300;

	/** Used to store all per-frame rendering data for a fixed maximum number of model renders **/
	private final int indirectVBO, drawDataVBO;
	private final int offsetsSize;
	private final ByteBuffer drawBuffer;
	private final ByteBuffer indirectBuffer;

	public ModelRenderProgramEnhanced(Shader vertexShader, Shader fragmentShader, int textureAtlas, ModelData... models) {
		super(vertexShader, fragmentShader, textureAtlas, models);
		//https://learnopengl.com/Advanced-OpenGL/Advanced-GLSL
		IntBuffer params = BufferUtils.createIntBuffer(2);
		IntBuffer props = BufferUtils.createIntBuffer(2)
		  .put(GL_BUFFER_BINDING).put(GL_BUFFER_DATA_SIZE).flip();
		int instanceDataIndex = glGetProgramResourceIndex(programID, GL_UNIFORM_BLOCK, "instance_data");
		glGetProgramResourceiv(programID, GL_UNIFORM_BLOCK, instanceDataIndex, props, null, params);
//		IntBuffer props2 = BufferUtils.createIntBuffer(1).put(GL_ACTIVE_VARIABLES).flip();
//		IntBuffer params2 = BufferUtils.createIntBuffer(params.get(2));
//		glGetProgramResourceiv(programID, GL_UNIFORM_BLOCK, instanceDataIndex, props2, null, params2);
//		while(params2.hasRemaining()) {
//			int index = params2.get();
//			IntBuffer out = BufferUtils.createIntBuffer(2);
//			glGetProgramResourceiv(programID, GL_UNIFORM, index,
//				BufferUtils.createIntBuffer(2).put(GL_OFFSET).put(GL_ARRAY_SIZE).flip(), null, out);
//			//System.out.println("Variable " + index + ": "
//			+ glGetProgramResourceName(programID, GL_UNIFORM, index) + " offset " + out.get(0) + " array size " + out.get(1));
//		}
		int instanceDataBinding = params.get(0);
		int bufferSize = params.get(1);

		//draw data layout:
		//1. all indirect structs (GL_DRAW_INDIRECT_BUFFER)
		//2. offsets into glsl instance structs for the start of each gl_DrawID
		//3. all glsl instance structs (GL_UNIFORM_BUFFER)
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
	public void render(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		queueRender(model, opacity, posX, posY, scaleX, scaleY, rotation);
		render();
	}

	@Override
	public void render() {
		while(!renderQueue.isEmpty()) {
			int renderIndex = 0, commandIndex = 0;
			while(renderIndex < MAX_RENDER_COUNT && !renderQueue.isEmpty()) {
				RenderInstance render = renderQueue.poll();
				putRenderInstance(drawBuffer.position(offsetsSize + renderIndex * INSTANCE_STRUCT_SIZE), render);
				int instanceCount = 1;
				while(renderIndex + instanceCount < MAX_RENDER_COUNT && !renderQueue.isEmpty() && render.model().equals(renderQueue.peek().model())) {
					putRenderInstance(drawBuffer, renderQueue.poll());
					instanceCount++;
				}

				//insert the starting index of the indirect command's transforms
				drawBuffer
					.position(commandIndex * OFFSET_SIZE)
					.putInt(renderIndex);

				ModelAttributes model = modelProperties.get(render.model());

				indirectBuffer
					.putInt(model.indexCount())
					.putInt(instanceCount)
					.putInt(model.indexOffset())
					.putInt(0)
					.putInt(0);
				commandIndex++;
				renderIndex += instanceCount;
			}

			//upload all the data to the GPU
			glBindBuffer(GL_UNIFORM_BUFFER, drawDataVBO);
			glBufferSubData(GL_UNIFORM_BUFFER, 0, drawBuffer.position(0).limit(offsetsSize + renderIndex * INSTANCE_STRUCT_SIZE));
			drawBuffer.clear();
			glBindVertexArray(vaoID);
			glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVBO);
			glBufferSubData(GL_DRAW_INDIRECT_BUFFER, 0, indirectBuffer.flip());
			indirectBuffer.clear();
			glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, 0, commandIndex, 0);
		}
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