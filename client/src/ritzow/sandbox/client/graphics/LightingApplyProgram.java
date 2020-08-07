package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL46C.*;

public class LightingApplyProgram extends ShaderProgram {
	private final int vao, diffuse, lighting;

	public LightingApplyProgram(Shader vertex, Shader geometry, Shader fragment) {
		super(vertex, geometry, fragment);
		vao = glGenVertexArrays();
		diffuse = getUniformLocation("diffuse");
		lighting = getUniformLocation("lighting");
		setInteger(diffuse, 0);
		setInteger(lighting, 1);
	}

	public void render(OpenGLTexture diffuse, OpenGLTexture lighting) {
		setCurrent();
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, diffuse.id);

		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GL_TEXTURE_2D, lighting.id);

		glBindVertexArray(vao);
		glDrawArrays(GL_POINTS, 0, 1);
	}

	@Override
	public void delete() {
		super.delete();
		glDeleteVertexArrays(vao);
	}
}
