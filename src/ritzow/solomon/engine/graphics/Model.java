package ritzow.solomon.engine.graphics;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class Model {
//	protected final int 						vao;
//	protected final int 						vertexCount;
//	protected final PositionBuffer 				positions;
//	protected final Texture 					texture;
//	protected final TextureCoordinateBuffer 	textureCoords;
//	protected final IndexBuffer 				indices;
	
	protected final int vao;
	protected final int vertexCount;
	protected final int indices;
	protected final int positions;
	protected final int textureCoordinates;
	protected final int texture;
	
	protected Model(int vertexCount, int indices, int positions, int textureCoordinates, int texture) {
		super();
		this.vao = glGenVertexArrays();
		this.vertexCount = vertexCount;
		this.indices = indices;
		this.positions = positions;
		this.textureCoordinates = textureCoordinates;
		this.texture = texture;
		
		//bind the vao and specify its layout
		glBindVertexArray(vao);
		glBindBuffer(GL_ARRAY_BUFFER, positions);
		glEnableVertexAttribArray(ShaderProgram.ATTRIBUTE_POSITIONS);
		glVertexAttribPointer(ShaderProgram.ATTRIBUTE_POSITIONS, 2, GL_FLOAT, false, 0, 0);
		glBindBuffer(GL_ARRAY_BUFFER, textureCoordinates);
		glEnableVertexAttribArray(ShaderProgram.ATTRIBUTE_TEXTURE_COORDS);
		glVertexAttribPointer(ShaderProgram.ATTRIBUTE_TEXTURE_COORDS, 2, GL_FLOAT, false, 0, 0);
		glBindVertexArray(0);
	}

//	public Model(int vertexCount, PositionBuffer positions, Texture texture, TextureCoordinateBuffer textureCoords, IndexBuffer indices) {
//		this.vao = glGenVertexArrays();
//		this.vertexCount = vertexCount;
//		this.positions = positions;
//		this.texture = texture;
//		this.textureCoords = textureCoords;
//		this.indices = indices;
//		setup();
//	}
	
//	public Texture getTexture() {
//		return texture;
//	}
//	
//	public void setup() {
//		glBindVertexArray(vao);
//		positions.specifyFormat();
//		textureCoords.specifyFormat();
//		glBindVertexArray(0);
//	}
//	
//	public void bind() {
//		glBindVertexArray(vao);
//		indices.bind();
//	}
//	
//	public void render() {
//		glBindVertexArray(vao);
//		indices.bind();
//		texture.bind();
//		glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
//	}
//	
//	public void delete() {
//		positions.delete();
//		texture.delete();
//		textureCoords.delete();
//		indices.delete();
//		glDeleteVertexArrays(vao);
//	}
}
