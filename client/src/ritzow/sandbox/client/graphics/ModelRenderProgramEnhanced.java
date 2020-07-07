package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL46C.*;

public final class ModelRenderProgramEnhanced extends ModelRenderProgram {
	private static final int INSTANCE_DATA_UNIFORM_BLOCK_BINDING = 1;

	private static final int INDIRECT_STRUCT_SIZE = 5 * Integer.BYTES;

	/** Used to store all per-frame rendering data for a fixed maximum number of model renders **/
	private final int drawDataVBO;
	private final int maxModelCount, bufferSize, offsetsOffset, instancesOffset;
	private final ByteBuffer draw_data_buffer;
	private final ByteBuffer indirect_view;
	private final IntBuffer offset_view;
	private final FloatBuffer instance_view;
	//private final ByteBuffer indirect_data_buffer;
	//private final FloatBuffer instance_data_buffer;
	//private final IntBuffer instance_data_counts;
	//private final PointerBuffer instance_data_offsets;

	private int indirectsSize() {
		return maxModelCount * INDIRECT_STRUCT_SIZE;
	}

	private int offsetsSize() {
		return maxModelCount * Integer.BYTES; //offsets are ints
	}

	private int instancesSize() {
		return maxModelCount * 17 * Float.BYTES; //4x4 matrix and opacity, all floats
	}

	public ModelRenderProgramEnhanced(Shader vertexShader, Shader fragmentShader,
					int textureAtlas, ModelData... models) {
		super(vertexShader, fragmentShader, textureAtlas, models);
		//https://learnopengl.com/Advanced-OpenGL/Advanced-GLSL
		maxModelCount = 8; //TODO get this from the shader data
		offsetsOffset = indirectsSize();
		int offsetsEnd = offsetsOffset + offsetsSize();
		int uniformAlignment = glGetInteger(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);
		instancesOffset = offsetsEnd + (uniformAlignment - (offsetsEnd % uniformAlignment));
		bufferSize = instancesOffset + instancesSize();
		//draw data layout:
		//1. all indirect structs (GL_DRAW_INDIRECT_BUFFER)
		//2. offsets into glsl instance structs for the start of each gl_DrawID
		//3. all glsl instance structs (GL_UNIFORM_BUFFER)
		drawDataVBO = glCreateBuffers(); //glGenBuffers();
		glNamedBufferData(drawDataVBO, bufferSize, GL_DYNAMIC_DRAW);
		GraphicsUtility.checkErrors();
		this.draw_data_buffer = BufferUtils.createByteBuffer(bufferSize);
		this.indirect_view = draw_data_buffer.slice(0, offsetsOffset);
		this.offset_view = draw_data_buffer.slice(offsetsOffset, offsetsEnd).asIntBuffer();
		this.instance_view = draw_data_buffer.slice(instancesOffset, instancesSize()).asFloatBuffer();
		glBindBufferRange(GL_UNIFORM_BUFFER, INSTANCE_DATA_UNIFORM_BLOCK_BINDING, drawDataVBO, instancesOffset, instancesSize());
		//glUniformBlockBinding(this.programID, glGetUniformBlockIndex(this.programID, "instance_data"), INSTANCE_DATA_UNIFORM_BLOCK_BINDING);
		System.out.println(glGetInteger(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT));
		GraphicsUtility.checkErrors();
	}

	@Override
	public void render() {
		//glMultiDrawElementsIndirect (or without indirect) works even better but requires OpenGL 4.3
		//gl_DrawID can be used from glsl for this
		//https://www.khronos.org/opengl/wiki/Built-in_Variable_(GLSL)#Vertex_shader_inputs

		while(!renderQueue.isEmpty()) {
			int drawCount = Math.min(renderQueue.size(), maxModelCount);
			int offset = 0;
			for(int i = 0; i < drawCount; i++) {
				RenderInstance render = renderQueue.poll();
				ModelAttributes model = modelProperties.get(render.model());
				int instanceCount = 1;
				indirect_view
					.putInt(model.indexCount())
					.putInt(instanceCount /* 1 instance for now */)
					.putInt(model.indexOffset())
					.putInt(0)
					.putInt(0);
				prepInstanceData(instance_view.put(render.opacity()), render.posX(), render.posY(), render.scaleX(), render.scaleY(), render.rotation());
				offset_view.put(offset);
				offset += instanceCount;
			}
			//upload all the transformation matrices to the GPU before drawing
			glNamedBufferSubData(drawDataVBO, 0, draw_data_buffer.limit(instancesOffset + drawCount * 17 * Float.BYTES));
			//TODO do I need to clear after this?
			glBindVertexArray(vaoID);
			glBindBufferRange(GL_DRAW_INDIRECT_BUFFER, 0 /* TODO figure out binding points */, drawDataVBO, 0, drawCount * INDIRECT_STRUCT_SIZE);
			glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, 0, drawCount, 0);
			indirect_view.clear();
			offset_view.clear();
			instance_view.clear();
		}
	}
}