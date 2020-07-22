package ritzow.sandbox.client.graphics;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL46C.*;

//TODO revert this and optimize it for single glDrawElements calls, make queueRender just render each individually or something.
//TODO create common superclass for ModelRenderProgram460 and ModelRenderProgram450
public class ModelRenderProgramOld extends ModelRenderProgramBase {
	protected final int uniform_transform;
	private float lastOpacity = 0;
	private final FloatBuffer transform;

	ModelRenderProgramOld(Shader vertexShader, Shader fragmentShader,
						  int textureAtlas, ModelData... models) {
		super(vertexShader, fragmentShader, textureAtlas, models);
		this.uniform_transform = getUniformLocation("transform");
		this.transform = BufferUtils.createFloatBuffer(16);
	}

	@Override
	public void queueRender(Model model, float opacity,
							float posX, float posY, float scaleX, float scaleY,
							float rotation) {
		if(opacity != lastOpacity) {
			setFloat(uniform_opacity, opacity);
			lastOpacity = opacity;
		}
		glUniformMatrix4fv(uniform_transform, false,
			prepInstanceData(transform, posX, posY, scaleX, scaleY, rotation).flip());
		transform.clear();
		ModelAttributes modelAttribs = (ModelAttributes)model;
		glBindVertexArray(vaoID);
		glDrawElements(GL_TRIANGLES, modelAttribs.indexCount(), GL_UNSIGNED_INT, modelAttribs.indexOffset() * Integer.BYTES);
	}

	private static FloatBuffer prepInstanceData(FloatBuffer dest, float posX, float posY,
								 float scaleX, float scaleY, float rotation) {
		float rotX = (float)Math.cos(rotation);
		float rotY = (float)Math.sin(rotation);
		return dest
				.put(scaleX * rotX)	.put(scaleX * -rotY).put(0).put(0) //column major
				.put(scaleY * rotY)	.put(scaleY * rotX)	.put(0).put(0)
				.put(0)				.put(0)				.put(0).put(0)
				.put(posX)			.put(posY)			.put(0).put(1);
	}

	@Override
	public void flush() {}
}