package ritzow.solomon.engine.graphics.data;

import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

public class IndexBuffer {
	
	protected int id;
	protected int indices;
	
	public IndexBuffer(int[] indices) {
		this.id = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
	}
	
	public void bind() {
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
	}

	public void delete() {
		glDeleteBuffers(id);
	}
}
