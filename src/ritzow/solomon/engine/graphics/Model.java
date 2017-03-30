package ritzow.solomon.engine.graphics;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class Model {
	public final int vao;
	public final int vertexCount;
	public final int indices;
	public final int positions;
	public final int textureCoordinates;
	public final int texture;
	
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
		glEnableVertexAttribArray(RendererConstants.ATTRIBUTE_POSITIONS);
		glVertexAttribPointer(RendererConstants.ATTRIBUTE_POSITIONS, 2, GL_FLOAT, false, 0, 0);
		
		glBindBuffer(GL_ARRAY_BUFFER, textureCoordinates);
		glEnableVertexAttribArray(RendererConstants.ATTRIBUTE_TEXTURE_COORDS);
		glVertexAttribPointer(RendererConstants.ATTRIBUTE_TEXTURE_COORDS, 2, GL_FLOAT, false, 0, 0);
		glBindVertexArray(0);
		
		GraphicsUtility.checkErrors();
	}
}
