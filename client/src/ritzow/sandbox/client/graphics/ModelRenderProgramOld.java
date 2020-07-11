package ritzow.sandbox.client.graphics;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL46C.*;

//TODO revert this and optimize it for single glDrawElements calls, make queueRender just render each individually or something.
//TODO create common superclass for ModelRenderProgram460 and ModelRenderProgram450
public class ModelRenderProgramOld extends ModelRenderProgramBase {
	protected final int uniform_transform;
	protected final Queue<RenderInstance> renderQueue;
	private float lastOpacity = 0;

	ModelRenderProgramOld(Shader vertexShader, Shader fragmentShader,
						  int textureAtlas, ModelData... models) {
		super(vertexShader, fragmentShader, textureAtlas, models);
		this.uniform_transform = getUniformLocation("transform");
		setInteger(getUniformLocation("atlasSampler"), ATLAS_TEXTURE_UNIT);
		this.renderQueue = new ArrayDeque<RenderInstance>();
	}

	@Override
	public void queueRender(Model model, float opacity,
							float posX, float posY, float scaleX, float scaleY,
							float rotation) {
		renderQueue.add(new RenderInstance(model, opacity, posX, posY, scaleX, scaleY, rotation));
	}

	@Override
	public void flush() { //TODO move this code to queueRender and stop using a queue.
		try(MemoryStack stack = MemoryStack.stackPush()) {
			FloatBuffer instanceData = stack.mallocFloat(16);
			glBindVertexArray(vaoID);
			while(!renderQueue.isEmpty()) {
				RenderInstance render = renderQueue.poll();
				ModelAttributes model = modelProperties.get(render.model());
				prepInstanceData(instanceData, render.posX(), render.posY(), render.scaleX(), render.scaleY(), render.rotation());
				glUniformMatrix4fv(uniform_transform, false, instanceData.flip());
				if(render.opacity() != lastOpacity) {
					setFloat(uniform_opacity, render.opacity());
					lastOpacity = render.opacity();
				}
				glDrawElements(GL_TRIANGLES, model.indexCount(), GL_UNSIGNED_INT, model.indexOffset() * Integer.BYTES);
			}
		}
	}

	private static void prepInstanceData(FloatBuffer dest, float posX, float posY,
								 float scaleX, float scaleY, float rotation) {
		float rotX = (float)Math.cos(rotation);
		float rotY = (float)Math.sin(rotation);
		dest.put(scaleX * rotX)	.put(scaleX * -rotY)	.put(0).put(0) //column major
			.put(scaleY * rotY)	.put(scaleY * rotX)		.put(0).put(0)
			.put(0)				.put(0)					.put(0).put(0)
			.put(posX)			.put(posY)				.put(0).put(1);
	}
}