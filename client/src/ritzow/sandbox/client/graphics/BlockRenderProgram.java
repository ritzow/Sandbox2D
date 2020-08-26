package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import org.lwjgl.BufferUtils;
import ritzow.sandbox.client.graphics.ModelStorage.ModelBuffers;
import ritzow.sandbox.client.graphics.ModelStorage.ModelAttributes;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.world.block.ClientBlockProperties;

import static org.lwjgl.opengl.GL46C.*;

public class BlockRenderProgram extends ShaderProgram {
	private final int vao, uniformExposure, uniformView, uniformBlockGrid, instanceDataBinding;

	private final int indirectVBO, offsetsVBO, atlasTexture;
	private final ByteBuffer offsetsBuffer;
	private final ByteBuffer indirectBuffer;
	private Model currentModel;
	private int
		blockIndex,   //total number of model instances queued since last finish() call
		commandIndex,  //current instanced draw command, single draw ID
		instanceCount; //instance count for the current model aka for the current draw ID

	private static final int ATLAS_TEXTURE_UNIT = GL_TEXTURE0;
	private static final int MAX_RENDER_COUNT = 600;
	private static final int INDIRECT_STRUCT_SIZE = 5 * Integer.BYTES;
	private static final int INDIRECT_BUFFER_SIZE = MAX_RENDER_COUNT * INDIRECT_STRUCT_SIZE;
	//private static final int OFFSET_SIZE = Integer.BYTES * 4;
	//private static final int OFFSETS_BUFFER_SIZE =  MAX_RENDER_COUNT * OFFSET_SIZE;

	protected final float[] viewMatrix = {
		0, 0, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, 1,
	};

	public BlockRenderProgram(Shader vertex, Shader fragment, ModelBuffers models, int atlasTexture) {
		super(vertex, fragment);
		this.vao = ModelStorage.defineVAO(models, getAttributeLocation("position"), getAttributeLocation("textureCoord"));
		this.uniformExposure = getUniformLocation("exposure");
		this.uniformView = getUniformLocation("view");
		this.uniformBlockGrid = getUniformLocation("blockGrid");
		this.atlasTexture = atlasTexture;

		IntBuffer params = BufferUtils.createIntBuffer(2);
		IntBuffer props = BufferUtils.createIntBuffer(2)
			.put(GL_BUFFER_BINDING).put(GL_BUFFER_DATA_SIZE).flip();
		int instanceDataIndex = glGetProgramResourceIndex(programID, GL_UNIFORM_BLOCK, "instance_data");
		glGetProgramResourceiv(programID, GL_UNIFORM_BLOCK, instanceDataIndex, props, null, params);
		instanceDataBinding = params.get(0);
		int bufferSize = params.get(1);

		//initialize draw command buffer
		indirectVBO = glGenBuffers();
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVBO);
		glBufferStorage(GL_DRAW_INDIRECT_BUFFER, INDIRECT_BUFFER_SIZE, GL_DYNAMIC_STORAGE_BIT);
		this.indirectBuffer = BufferUtils.createByteBuffer(INDIRECT_BUFFER_SIZE);

		//initialize shader uniform data buffer for offsets
		offsetsVBO = glGenBuffers();
		glBindBuffer(GL_UNIFORM_BUFFER, offsetsVBO);
		glBufferStorage(GL_UNIFORM_BUFFER, bufferSize, GL_DYNAMIC_STORAGE_BIT);
		//connect the UBO to the shader instance_data block by binding it to binding point specified in the shader
		this.offsetsBuffer = BufferUtils.createByteBuffer(bufferSize);

		GraphicsUtility.checkErrors();
	}

	public void prepare() {
		setCurrent();
		glActiveTexture(ATLAS_TEXTURE_UNIT);
		glBindTexture(GL_TEXTURE_2D, atlasTexture);
		glBindVertexArray(vao);
		glBindBufferBase(GL_UNIFORM_BUFFER, instanceDataBinding, offsetsVBO);
	}

	public void setBounds(int leftX, int bottomY, int width) {
		setVectorUnsigned(uniformBlockGrid, leftX, bottomY, width);
	}

	public void setView(Camera camera, int width, int height) {
		ClientUtility.setViewMatrix(viewMatrix, camera, width, height);
		setMatrix(uniformView, viewMatrix);
	}

	public void setExposure(float exposure) {
		setFloat(uniformExposure, exposure);
	}

	//TODO difference between unbuffered model sequence and unrendererd models

	//can have a maximum number of block sequences (aka commands)

	private boolean unbufferedModels() {
		return instanceCount > 0;
	}

	private boolean unrenderedModels() {
		return currentModel != null;
	}

	private boolean bufferFull() {
		return commandIndex == MAX_RENDER_COUNT;
	}

	public void queue(ClientBlockProperties block) {
		//TODO automatically finish if grid is full (ie called queue or skip for the entire rectangle of blocks
		Model nextModel = Objects.requireNonNull(block.getModel());
		if(unbufferedModels()) {
			if(nextModel.equals(currentModel)) {
				instanceCount++;
			} else {
				nextCommand();
				if(bufferFull()) {
					render();
					commandIndex = 0;
				}
				currentModel = nextModel;
				instanceCount = 1;
			}
		} else {
			currentModel = nextModel;
			instanceCount = 1;
		}
	}

	public void skip(int count) {
		if(unbufferedModels()) {
			nextCommand();
			if(bufferFull()) {
				render();
				commandIndex = 0;
				currentModel = null;
			}
			instanceCount = 0;
		}
		blockIndex += count;
	}

	public void finish() {
		if(unbufferedModels()) {
			nextCommand();
		}

		if(unrenderedModels()) {
			render();
			commandIndex = 0;
			instanceCount = 0;
			currentModel = null;
		}
		blockIndex = 0; //reset grid position
	}

	private void nextCommand() {
		offsetsBuffer.putInt(blockIndex).putInt(0).putInt(0).putInt(0); //padding
		ModelAttributes properties = (ModelAttributes)currentModel;
		indirectBuffer
			.putInt(properties.indexCount())
			.putInt(instanceCount)
			.putInt(properties.indexOffset())
			.putInt(0) //baseVertex is always 0
			.putInt(0); //baseInstance is always 0
		commandIndex++;
		blockIndex += instanceCount;
	}

	//Render everything in the buffers
	private void render() {
		glBindBuffer(GL_UNIFORM_BUFFER, offsetsVBO);
		glBufferSubData(GL_UNIFORM_BUFFER, 0, offsetsBuffer.flip());
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVBO);
		glBufferSubData(GL_DRAW_INDIRECT_BUFFER, 0, indirectBuffer.flip());
		glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, 0, commandIndex, 0);
		offsetsBuffer.clear();
		indirectBuffer.clear();
	}
}
