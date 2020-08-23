package ritzow.sandbox.client.graphics;

import ritzow.sandbox.client.util.ClientUtility;

import static org.lwjgl.opengl.GL46C.*;

public class ShadingApplyProgram extends ShaderProgram {
	private static final int BLOCK_LIGHTING_TEXTURE_UNIT = GL_TEXTURE0;

	private final int vao, uniformView;

	public ShadingApplyProgram(Shader vertex, Shader geometry, Shader fragment) {
		super(vertex, geometry, fragment);
		vao = glGenVertexArrays();
		uniformView = getUniformLocation("view");
	}

	public void render(OpenGLTexture lighting, Camera camera, int width, int height) {
		setCurrent();
		setVector(uniformView,
			ClientUtility.getViewLeftBound(camera, width, height) + 0.5f,
			ClientUtility.getViewBottomBound(camera, width, height) + 0.5f,
			ClientUtility.getViewRightBound(camera, width, height) + 0.5f,
			ClientUtility.getViewTopBound(camera, width, height) + 0.5f
		);
		glActiveTexture(BLOCK_LIGHTING_TEXTURE_UNIT);
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
