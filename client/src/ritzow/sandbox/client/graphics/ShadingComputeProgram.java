package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL46C.*;

public class ShadingComputeProgram extends ShaderProgram {
	private static final int
		DIFFUSE_IMAGE_UNIT = 0,
		SHADING_IMAGE_UNIT = 1;

	private final int uniformBounds;

	public ShadingComputeProgram(Shader compute) {
		super(compute);
		uniformBounds = getUniformLocation("bounds");
	}

	public void render(OpenGLTexture diffuse, OpenGLTexture shading, int left, int bottom, int right, int top) {
		setCurrent();
		left -= 2;
		bottom -= 2;
		right += 2;
		top += 2;
		setVectorUnsigned(uniformBounds, left, bottom);
		glBindImageTexture(DIFFUSE_IMAGE_UNIT, diffuse.id, 0, false, 0, GL_READ_ONLY, GL_R8UI);
		glBindImageTexture(SHADING_IMAGE_UNIT, shading.id, 0, false, 0, GL_WRITE_ONLY, GL_R8UI);
		glDispatchCompute(right - left, top - bottom, 1);
	}

}
