package graphics.data;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import graphics.ShaderProgram;
import java.nio.FloatBuffer;

public class TextureCoordinateBuffer extends Buffer {

	protected FloatBuffer data;
	
	public TextureCoordinateBuffer(float[] coords) {
		this.objectID = glGenBuffers();
		this.data = storeArrayInBuffer(coords);
		buffer();		
	}
	
	public void buffer() {
		bind();
		glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
		unbind();
	}
	
	public void specifyAttributeFormat() {
		bind();
		glEnableVertexAttribArray(ShaderProgram.ATTRIBUTE_TEXTURE_COORDS);
		glVertexAttribPointer(ShaderProgram.ATTRIBUTE_TEXTURE_COORDS, 2, GL_FLOAT, false, 0, 0);
		unbind();
	}

	public void bind() {
		glBindBuffer(GL_ARRAY_BUFFER, objectID);
	}

	public void unbind() {
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	public void delete() {
		glDeleteBuffers(objectID);
		data.clear();
	}
}
