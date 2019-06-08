package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL46C.*;

import java.util.HashMap;
import java.util.Map;

public final class ModelRenderProgram extends ShaderProgram {

	private final int
		uniform_transform,
		uniform_opacity,
		uniform_view,
		textureUnit;

	private final int atlasTexture;

	private static final float[] identityMatrix = {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};

	private final float[] aspectMatrix = {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};

	private final float[] cameraMatrix = {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};

	private final float[] viewMatrix = {
			0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 0,
	};

	private final float[] transformationMatrix = {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};

	private final Map<Integer, RenderData> models;

	public ModelRenderProgram(Shader vertexShader, Shader fragmentShader, int textureAtlas) {
		super(vertexShader, fragmentShader);
		models = new HashMap<>();
		//glGetAttribLocation(program, name) TODO use getattriblocation? needs to be updated in VAO creation
		this.uniform_transform = getUniformLocation("transform");
		this.uniform_opacity = getUniformLocation("opacity");
		this.uniform_view = getUniformLocation("view");
		this.atlasTexture = textureAtlas;
		this.textureUnit = 0;
		setCurrent(); //needs this for setSamplerIndex which uses current program
		setSamplerIndex(getUniformLocation("textureSampler"), textureUnit);
		GraphicsUtility.checkErrors();
	}

	public void register(int modelID, RenderData model) {
		if(models.containsKey(modelID))
			throw new IllegalArgumentException("modelID already in use");
		models.put(modelID, model);
	}

	/**
	 * Sets the opacity that will be used when the program renders models
	 * @param opacity
	 */
	public void loadOpacity(float opacity) {
		setFloat(uniform_opacity, opacity);
	}

	@Override
	public void setCurrent() {
		super.setCurrent();
		setTexture(textureUnit, atlasTexture);
	}

	/**
	 * Renders a model using the current shader program properties
	 * @param model the model to render
	 */
	public void renderVAO(int modelID) {
		RenderData model = models.get(modelID);
		if(model == null)
			throw new IllegalArgumentException("no model exists for model id " + modelID);
		glBindVertexArray(model.vao);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, model.indices);
		//TODO instanced rendering allows multiple runs of the shader program with 1 draw call
		glDrawElements(GL_TRIANGLES, model.vertexCount, GL_UNSIGNED_INT, 0);
	}

	/**
	 * Renders a model with the specified properties
	 * @param model the model to render
	 * @param opacity the opacity of the model
	 * @param posX the horizontal position to render the model
	 * @param posY the vertical position to render the model
	 * @param scaleX the horizontal stretch scale of the model
	 * @param scaleY the vertical stretch scale of the model
	 * @param rotation the rotation, in radians, of the model
	 */
	public void render(int modelID, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		loadOpacity(opacity);
		loadTransformationMatrix(posX, posY, scaleX, scaleY, rotation);
		renderVAO(modelID);
	}

	public void render(Graphics g, float posX, float posY) {
		render(g.getModelID(), g.getOpacity(), posX, posY, g.getScaleX(), g.getScaleY(), g.getRotation());
	}

	public void render(Graphics g, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		render(g.getModelID(), g.getOpacity() * opacity, posX, posY, g.getScaleX() * scaleX, g.getScaleY() * scaleY, g.getRotation() + rotation);
	}

	public void loadTransformationMatrix(float posX, float posY, float scaleX, float scaleY, float rotation) {
		transformationMatrix[0] = scaleX * (float) Math.cos(rotation);
		transformationMatrix[1] = scaleY * (float) Math.sin(rotation);
		transformationMatrix[3] = posX;
		transformationMatrix[4] = scaleX * (float) -Math.sin(rotation);
		transformationMatrix[5] = scaleY * (float) Math.cos(rotation);
		transformationMatrix[7] = posY;
		setMatrix(uniform_transform, transformationMatrix);
	}

	public void loadViewMatrixIdentity() {
		setMatrix(uniform_view, identityMatrix);
	}

	public void loadViewMatrix(float framebufferWidth, float framebufferHeight) {
		aspectMatrix[0] = framebufferHeight/framebufferWidth;
		setMatrix(uniform_view, aspectMatrix);
	}
	
	private static void loadCameraMatrix(float[] matrix, Camera camera) {
		matrix[0] = camera.getZoom();
		matrix[3] = -camera.getPositionX() * camera.getZoom();
		matrix[5] = camera.getZoom();
		matrix[7] = -camera.getPositionY() * camera.getZoom();
	}

	public void loadViewMatrix(Camera camera, float framebufferWidth, float framebufferHeight) {
		aspectMatrix[0] = framebufferHeight/framebufferWidth;
		loadCameraMatrix(cameraMatrix, camera);
		multiply(aspectMatrix, cameraMatrix, viewMatrix);
		setMatrix(uniform_view, viewMatrix);
	}

	//matrix operations on 4x4 1-dimensional arrays
	private static void multiply(float[] a, float[] b, float[] destination) {
		for(int i = 0; i <= 12; i += 4) {
			for(int j = 0; j < 4; j++) {
				destination[i + j] = a[i] * b[j] 
								   + a[i + 1] * b[j + 4] 
								   + a[i + 2] * b[j + 8] 
								   + a[i + 3] * b[j + 12];
			}
		}
	}

//	private static float get(float[] array, int row, int column) {
//		return array[(row * 4) + column];
//	}
//
//	private static void set(float[] array, int row, int column, float value) {
//		array[(row * 4) + column] =  value;
//	}
}