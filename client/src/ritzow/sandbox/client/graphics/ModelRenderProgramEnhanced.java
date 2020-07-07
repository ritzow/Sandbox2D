package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

import static org.lwjgl.opengl.GL46C.*;

public final class ModelRenderProgramEnhanced extends ModelRenderProgram {
	private static final int INSTANCE_DATA_UNIFORM_BLOCK_BINDING = 1;

	private static final int INDIRECT_STRUCT_SIZE = -1;

	/** Used to store all per-frame rendering data for a fixed maximum number of model renders **/
	private final int drawDataVBO;
	private final int maxModelCount, bufferSize;
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
		bufferSize = indirectsSize() + offsetsSize() + instancesSize();
		//draw data layout:
		//1. all indirect structs (GL_DRAW_INDIRECT_BUFFER)
		//2. offsets into glsl instance structs for the start of each gl_DrawID
		//3. all glsl instance structs (GL_UNIFORM_BUFFER)
		drawDataVBO = glGenBuffers();
		glNamedBufferData(drawDataVBO, bufferSize, GL_DYNAMIC_DRAW);
		this.draw_data_buffer = BufferUtils.createByteBuffer(bufferSize);
		this.indirect_view = draw_data_buffer.slice(0, indirectsSize());
		this.offset_view = draw_data_buffer.slice(indirectsSize(), offsetsSize()).asIntBuffer();
		this.instance_view = draw_data_buffer.slice(indirectsSize() + offsetsSize(), instancesSize()).asFloatBuffer();

		glBindBufferRange(GL_UNIFORM_BUFFER, drawDataVBO);
		//glBufferData(GL_UNIFORM_BUFFER, bufferSize, GL_DYNAMIC_DRAW);
		glUniformBlockBinding(this.programID, glGetUniformBlockIndex(this.programID, "instance_data"), INSTANCE_DATA_UNIFORM_BLOCK_BINDING);
		//glBindBuffer(GL_DRAW_INDIRECT_BUFFER, drawInfo);
		//glBindBufferRange();
		//this.instance_data_buffer = BufferUtils.createFloatBuffer(instance_data_size);
		//this.instance_data_counts = BufferUtils.createIntBuffer(instance_data_size);
		//this.instance_data_offsets = BufferUtils.createPointerBuffer(instance_data_size);
	}

	@Override
	public void render() {
		//glMultiDrawElementsIndirect (or without indirect) works even better but requires OpenGL 4.3
		//gl_DrawID can be used from glsl for this
		//https://www.khronos.org/opengl/wiki/Built-in_Variable_(GLSL)#Vertex_shader_inputs

		while(!renderQueue.isEmpty()) {
			int drawCount = Math.min(renderQueue.size(), maxModelCount);
			for(int i = 0; i < drawCount; i++) {
				RenderInstance render = renderQueue.poll();
				ModelAttributes model = modelProperties.get(render.model());
				prepInstanceData(instance_view, render.posX(), render.posY(), render.scaleX(), render.scaleY(), render.rotation());
				instance_data_offsets.put(model.indexOffset());
//				glUniformMatrix4fv(uniform_transform, false, instanceData.flip());
//				if(render.opacity() != lastOpacity) {
//					setFloat(uniform_opacity, render.opacity);
//					lastOpacity = render.opacity;
//				}
//				glDrawElements(GL_TRIANGLES, model.indexCount(), GL_UNSIGNED_INT, model.indexOffset() * Integer.BYTES);
			}
			//upload all the transformation matrices to the GPU before drawing
			glNamedBufferSubData(drawDataVBO, 0, draw_data_buffer.limit(indirectsSize() + offsetsSize() + drawCount * 17)); //TODO do I need to clear after this?
			//glBufferSubData(GL_UNIFORM_BUFFER, 0, instance_data_buffer.flip());
			//instance_data_buffer.clear();
			//TODO eventually should switch to glMultiDrawElementsIndirect with GL_DRAW_INDIRECT_BUFFER for instanced rendering
			glBindVertexArray(vaoID);
			glBindBufferRange(GL_DRAW_INDIRECT_BUFFER, 0, drawDataVBO, 0, indirectsSize());
			glBindBufferRange(GL_UNIFORM_BUFFER);
			glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, 0, drawCount, 0);
		}
	}
}