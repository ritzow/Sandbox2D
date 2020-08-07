package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import ritzow.sandbox.client.util.ClientUtility;

import static org.lwjgl.opengl.GL46C.*;

/**
 * Point lighting system:
 *
 * 1. The view information is set so that calls to queueLight can compute the proper stuff
 * 2. glDrawArrays with the proper light count is called
 * 3. Geometry shader produces a square for each light at the proper position in the screen space
 * 4. Shader interpolates relative position within light and sends to fragment shader
 * 5. Fragment shader computes all pixel colors within light square using the interpolated values
 */
public class LightRenderProgram extends ShaderProgram {
	private static final int MAX_LIGHTS = 100;
	private static final int STRIDE_BYTES = 2 * Float.BYTES + 3 * Float.BYTES + 1 * Float.BYTES; //pos, color, intensity
	private static final int OFFSET_POSITION = 0, OFFSET_COLOR = OFFSET_POSITION + 2 * Float.BYTES, OFFSET_INTENSITY = OFFSET_COLOR + 3 * Float.BYTES;

	private final ByteBuffer lightsBuffer;
	private final int vao, vbo, uniformView;
	private int count;

	public LightRenderProgram(Shader vertex, Shader geometry, Shader fragment) {
		super(vertex, geometry, fragment);
		vao = glGenVertexArrays();
		glBindVertexArray(vao);

		int lightsVBO = glGenBuffers();
		vbo = lightsVBO;
		glBindBuffer(GL_ARRAY_BUFFER, lightsVBO);
		int bufferSize = STRIDE_BYTES * MAX_LIGHTS;
		glBufferStorage(GL_ARRAY_BUFFER, bufferSize, GL_DYNAMIC_STORAGE_BIT);
		lightsBuffer = BufferUtils.createByteBuffer(bufferSize);

		//position
		int positionAttribute = getAttributeLocation("position");
		glEnableVertexAttribArray(positionAttribute);
		glVertexAttribPointer(positionAttribute, 2, GL_FLOAT, false, STRIDE_BYTES, OFFSET_POSITION);

		//color
		int colorAttribute = getAttributeLocation("color");
		glEnableVertexAttribArray(colorAttribute);
		glVertexAttribPointer(colorAttribute, 3, GL_FLOAT, false, STRIDE_BYTES, OFFSET_COLOR);

		//intensity
		int intensityAttribute = getAttributeLocation("intensity");
		glEnableVertexAttribArray(intensityAttribute);
		glVertexAttribPointer(intensityAttribute, 1, GL_FLOAT, false, STRIDE_BYTES, OFFSET_INTENSITY);

		this.uniformView = getUniformLocation("view");

		GraphicsUtility.checkErrors();
	}

	//private Camera camera;
	//private int frameWidth, frameHeight;

	private final float[] viewMatrix = {
		0, 0, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, 1,
	};

	public void setup(Camera camera, int frameWidth, int frameHeight) {
		//this.camera = camera;
		//this.frameWidth = frameWidth;
		//this.frameHeight = frameHeight;
		setCurrent();
		ClientUtility.setViewMatrix(viewMatrix, camera, frameWidth, frameHeight);
		setMatrix(uniformView, viewMatrix);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE);
		glBlendEquation(GL_FUNC_ADD);
		glBindVertexArray(vao);
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
	}

	public void queueLight(Light light, float offsetX, float offsetY) {
		//TODO transform posX, posY, and intensity
		//System.out.println("queue light at " + (light.posX() + offsetX) + ", " + (light.posY() + offsetY));
		lightsBuffer
			.putFloat(light.posX() + offsetX)
			.putFloat(light.posY() + offsetY)
			.putFloat(light.red()).putFloat(light.green()).putFloat(light.blue())
			.putFloat(light.intensity());
		count++;
		if(count == MAX_LIGHTS) {
			flush();
		}
	}

	public void flush() {
		glBufferSubData(GL_ARRAY_BUFFER, 0, lightsBuffer.flip());
		glDrawArrays(GL_POINTS, 0, count);
		count = 0;
		lightsBuffer.clear();
	}
}
