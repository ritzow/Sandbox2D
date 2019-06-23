package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL46C.*;

public final class RenderData {
	public final int vao;
	public final int vertexCount;
	public final int indices;
	public final int positions;
	public final int textureCoordinates;

	public RenderData(int vertexCount, int indices, int positions, int textureCoordinates) {
		this.vertexCount = vertexCount;
		this.indices = indices;
		this.positions = positions;
		this.textureCoordinates = textureCoordinates;
		this.vao = glGenVertexArrays();

		//bind the vao and specify its layout
		glBindVertexArray(vao);

		glBindBuffer(GL_ARRAY_BUFFER, positions);
		glEnableVertexAttribArray(RenderConstants.ATTRIBUTE_POSITIONS);
		glVertexAttribPointer(RenderConstants.ATTRIBUTE_POSITIONS, 2, GL_FLOAT, false, 0, 0);

		glBindBuffer(GL_ARRAY_BUFFER, textureCoordinates);
		glEnableVertexAttribArray(RenderConstants.ATTRIBUTE_TEXTURE_COORDS);
		glVertexAttribPointer(RenderConstants.ATTRIBUTE_TEXTURE_COORDS, 2, GL_FLOAT, false, 0, 0);

		glBindVertexArray(0);

		GraphicsUtility.checkErrors();
	}
}
