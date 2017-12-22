package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.glBindAttribLocation;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class ModelRenderProgram extends ShaderProgram {
	private final Camera camera;
	
	private final int 
		uniform_transform, 
		uniform_opacity, 
		uniform_view, 
		textureUnit;
	
	private float framebufferWidth, framebufferHeight;
	
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
	
	private final Map<Integer, Model> models;
	
	public ModelRenderProgram(Shader vertexShader, Shader fragmentShader, Camera camera) {
		super(vertexShader, fragmentShader);
		models = new HashMap<Integer, Model>();
		GraphicsUtility.checkProgramCompilation(this);
		glBindAttribLocation(programID, RendererConstants.ATTRIBUTE_POSITIONS, "position");
		glBindAttribLocation(programID, RendererConstants.ATTRIBUTE_TEXTURE_COORDS, "textureCoord");
		this.uniform_transform = getUniformID("transform");
		this.uniform_opacity = getUniformID("opacity");
		this.uniform_view = getUniformID("view");
		this.textureUnit = 0;
		this.camera = camera;
		setCurrent(); //needs this for setSamplerIndex which uses current program
		setSamplerIndex(getUniformID("textureSampler"), textureUnit);
		GraphicsUtility.checkErrors();
	}
	
	public void register(int modelID, Model model) {
		if(models.containsKey(modelID))
			throw new IllegalArgumentException("modelID already in use");
		models.put(modelID, model);
	}
	
	public Camera getCamera() {
		return camera;
	}
	
	/**
	 * Sets the opacity that will be used when the program renders models
	 * @param opacity
	 */
	public void loadOpacity(float opacity) {
		setFloat(uniform_opacity, opacity);
	}
	
	/**
	 * Renders a model using the current shader program properties
	 * @param model the model to render
	 */
	public void render(int modelID) {
		Model model = models.get(modelID);
		if(model == null)
			throw new IllegalArgumentException("no model exists for model id " + modelID);
		glBindVertexArray(model.vao);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, model.indices);
		setTexture(textureUnit, model.texture);
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
		render(modelID);
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
	
	public void loadViewMatrix(boolean includeCamera) {
		aspectMatrix[0] = framebufferHeight/framebufferWidth;
		if(includeCamera) {
			loadCameraMatrix(cameraMatrix);
			Arrays.fill(viewMatrix, 0);
			multiply(aspectMatrix, cameraMatrix, viewMatrix);
			setMatrix(uniform_view, viewMatrix);
		} else {
			setMatrix(uniform_view, aspectMatrix);
		}
	}
	
	private void loadCameraMatrix(float[] matrix) {
		matrix[0] = camera.getZoom();
		matrix[3] = -camera.getPositionX() * camera.getZoom();
		matrix[5] = camera.getZoom();
		matrix[7] = -camera.getPositionY() * camera.getZoom();
	}
	
	public float getWorldViewportLeftBound() {
		float worldX = -framebufferWidth/framebufferHeight; //far left of screen after accounting for aspect ratio, in world coordinates
		worldX /= camera.getZoom();
		worldX += camera.getPositionX();
		return worldX;
	}
	
	public float getWorldViewportRightBound() {
		float worldX = framebufferWidth/framebufferHeight; //far right of screen, after accounting for aspect ratio, in world coordinates
		worldX /= camera.getZoom();
		worldX += camera.getPositionX();
		return worldX;
	}
	
	public float getWorldViewportTopBound() {
		float worldY = 1;
		worldY /= camera.getZoom();
		worldY += camera.getPositionY();
		return worldY;
	}
	
	public float getWorldViewportBottomBound() {
		float worldY = -1;
		worldY /= camera.getZoom();
		worldY += camera.getPositionY();
		return worldY;
	}

	public void setResolution(float framebufferWidth, float framebufferHeight) {
		this.framebufferWidth = framebufferWidth;
		this.framebufferHeight = framebufferHeight;
	}
	
	//matrix operations on 1-dimensional arrays
	private static void multiply(float[] a, float[] b, float[] destination) {
		for(int i = 0; i < 4; i++){
			for(int j = 0; j < 4; j++){
				for(int k = 0; k < 4; k++){
					set(destination, i, j, get(destination, i, j) + get(a, i, k) * get(b, k, j));
				}
			}
		}
	}
	
	private static float get(float[] array, int row, int column) {
		return array[(row * 4) + column];
	}
	
	private static void set(float[] array, int row, int column, float value) {
		array[(row * 4) + column] =  value;
	}
}