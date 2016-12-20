package ritzow.solomon.engine.graphics;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import ritzow.solomon.engine.graphics.data.IndexBuffer;
import ritzow.solomon.engine.graphics.data.PositionBuffer;
import ritzow.solomon.engine.graphics.data.Texture;
import ritzow.solomon.engine.graphics.data.TextureCoordinateBuffer;

public class Model {
	protected final int 						vao;
	protected final int 						vertexCount;
	protected final PositionBuffer 				positions;
	protected final Texture 					texture;
	protected final TextureCoordinateBuffer 	textureCoords;
	protected final IndexBuffer 				indices;
	
	public Model(int vertexCount, PositionBuffer positions, Texture texture, TextureCoordinateBuffer textureCoords, IndexBuffer indices) {
		this.vao = glGenVertexArrays();
		this.vertexCount = vertexCount;
		this.positions = positions;
		this.texture = texture;
		this.textureCoords = textureCoords;
		this.indices = indices;
		setup();
	}
	
	public void setup() {
		glBindVertexArray(vao);
		positions.specifyFormat();
		textureCoords.specifyFormat();
		glBindVertexArray(0);
	}
	
	public void render() {
		glBindVertexArray(vao);
		indices.bind();
		texture.bind();
		glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
	}
	
	public void delete() {
		positions.delete();
		texture.delete();
		textureCoords.delete();
		indices.delete();
		glDeleteVertexArrays(vao);
	}
}
