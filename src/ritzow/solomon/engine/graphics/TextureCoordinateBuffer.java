package ritzow.solomon.engine.graphics;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

final class TextureCoordinateBuffer {
	protected final int id;
	
	public TextureCoordinateBuffer(float[] data) {
		this.id = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, id);
		glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
	}
	
//	public void specifyFormat() {
//		glBindBuffer(GL_ARRAY_BUFFER, id);
//		glEnableVertexAttribArray(ShaderProgram.ATTRIBUTE_TEXTURE_COORDS);
//		glVertexAttribPointer(ShaderProgram.ATTRIBUTE_TEXTURE_COORDS, 2, GL_FLOAT, false, 0, 0);
//	}

	public void delete() {
		glDeleteBuffers(id);
	}
}
