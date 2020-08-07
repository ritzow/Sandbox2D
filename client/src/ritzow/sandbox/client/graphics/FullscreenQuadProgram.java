package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL46C.*;

public class FullscreenQuadProgram extends ShaderProgram {
	private final int vao;

	public FullscreenQuadProgram(Shader vertex, Shader geometry, Shader fragment) {
		super(vertex, geometry, fragment);
		vao = glGenVertexArrays();
	}

	public void render(int sourceTexture, Framebuffer dest) {
		setCurrent();
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, sourceTexture);

		glBindVertexArray(vao);
		glDrawArrays(GL_POINTS, 0, 1);
	}

	@Override
	public void delete() {
		super.delete();
		glDeleteVertexArrays(vao);
	}
}
