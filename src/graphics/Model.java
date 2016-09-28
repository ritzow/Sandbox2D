package graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import graphics.data.IndexBuffer;
import graphics.data.PositionBuffer;
import graphics.data.Texture;
import graphics.data.TextureCoordinateBuffer;

public class Model {
	protected int 						vao;
	protected PositionBuffer 			positions;
	protected Texture 					texture;
	protected TextureCoordinateBuffer 	textureCoords;
	protected IndexBuffer 				indices;
	
	public Model(PositionBuffer positions, Texture texture, TextureCoordinateBuffer textureCoords, IndexBuffer indices) {
		this.vao = glGenVertexArrays();
		this.positions = positions;
		this.texture = texture;
		this.textureCoords = textureCoords;
		this.indices = indices;
		setup();
	}
	
	public void setTexture(Texture texture) {
		this.texture = texture;
	}
	
	public Texture getTexture() {
		return texture;
	}
	
	public void setup() {
		glBindVertexArray(vao);
		positions.specifyAttributeFormat();
		textureCoords.specifyAttributeFormat();
		glBindVertexArray(0);
	}
	
	public void render() {
		glBindVertexArray(vao);
		indices.bind();
		texture.bind();
		glDrawElements(GL_TRIANGLES, indices.getNumElements(), GL_UNSIGNED_INT, 0);
		glBindVertexArray(0);
	}
	
	public void delete() {
		positions.delete();
		texture.delete();
		textureCoords.delete();
		indices.delete();
		glDeleteVertexArrays(vao);
	}
}
