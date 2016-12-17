package graphics.data;

import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

import java.nio.IntBuffer;

public class IndexBuffer extends Buffer {
	
	protected IntBuffer data;
	protected int indices;
	
	public IndexBuffer(int[] indices) {
		this.objectID = glGenBuffers();
		this.data = storeArrayInBuffer(indices);
		this.indices = indices.length;
		buffer();
	}

	@Override
	public void buffer() {
		bind();
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, data, GL_STATIC_DRAW);
		unbind();
	}

	@Override
	public void bind() {
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, objectID);
	}

	@Override
	public void unbind() {
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	@Override
	public void delete() {
		glDeleteBuffers(objectID);
		data.clear();
	}
	
	public int getNumElements() {
		return indices;
	}
}
