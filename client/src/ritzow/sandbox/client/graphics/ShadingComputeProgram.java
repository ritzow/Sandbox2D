package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL46C.*;

public class ShadingComputeProgram extends ShaderProgram {
	private static final int DIFFUSE_IMAGE_UNIT = 0;

	private final int vao;

	public ShadingComputeProgram(Shader vertex, Shader geometry, Shader fragment) {
		super(vertex, geometry, fragment);
		vao = glGenVertexArrays();
	}

	public void render(OpenGLTexture diffuse) {
		setCurrent();
		glBindImageTexture(DIFFUSE_IMAGE_UNIT, diffuse.id, 0, false, 0, GL_READ_ONLY, GL_R8UI);
		glBindVertexArray(vao);
		glDrawArrays(GL_POINTS, 0, 1);
	}

	@Override
	public void delete() {
		super.delete();
		glDeleteVertexArrays(vao);
	}
}
