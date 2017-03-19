package ritzow.solomon.engine.world.base;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.glBindAttribLocation;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

import java.io.IOException;
import java.io.InputStream;
import ritzow.solomon.engine.graphics.Camera;
import ritzow.solomon.engine.graphics.Graphics;
import ritzow.solomon.engine.graphics.Model;
import ritzow.solomon.engine.graphics.Models;
import ritzow.solomon.engine.graphics.OpenGLException;
import ritzow.solomon.engine.graphics.RendererConstants;
import ritzow.solomon.engine.graphics.Shader;
import ritzow.solomon.engine.graphics.ShaderProgram;

public final class ModelRenderProgram extends ShaderProgram {
	private final Camera camera;
	private final int uniform_transform;
	private final int uniform_opacity;
	private final int uniform_view;
	
	protected float framebufferWidth;
	protected float framebufferHeight;
	
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
	
	//TODO store models in ModelRenderProgram, rather than Models class?
	
	public ModelRenderProgram(InputStream vertexShader, InputStream fragmentShader, Camera camera) throws IOException {
		super(
			new Shader(vertexShader, org.lwjgl.opengl.GL20.GL_VERTEX_SHADER), 
			new Shader(fragmentShader, org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER)
		);
		
		glBindAttribLocation(programID, RendererConstants.ATTRIBUTE_POSITIONS, "position");
		glBindAttribLocation(programID, RendererConstants.ATTRIBUTE_TEXTURE_COORDS, "textureCoord");
		
		this.camera = camera;
		this.uniform_transform = getUniformID("transform");
		this.uniform_opacity = getUniformID("opacity");
		this.uniform_view = getUniformID("view");
		
		OpenGLException.checkErrors();
	}
	
	public void loadOpacity(float opacity) {
		loadFloat(uniform_opacity, opacity);
	}
	
	@SuppressWarnings("static-method")
	public void render(Model model) {
		glBindVertexArray(model.vao);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, model.indices);
		glBindTexture(GL_TEXTURE_2D, model.texture);
		glDrawElements(GL_TRIANGLES, model.vertexCount, GL_UNSIGNED_INT, 0);
	}
	
	public void render(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		loadOpacity(opacity);
		loadTransformationMatrix(posX, posY, scaleX, scaleY, rotation);
		render(model);
	}
	
	public void renderGraphics(Graphics g, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		render(Models.forIndex(g.getModelIndex()), g.getOpacity() * opacity, posX, posY, g.getScaleX() * scaleX, g.getScaleY() * scaleY, g.getRotation() + rotation);
	}
	
	public void loadTransformationMatrix(float posX, float posY, float scaleX, float scaleY, float rotation) {
		transformationMatrix[0] = scaleX * (float) Math.cos(rotation);
		transformationMatrix[1] = scaleY * (float) Math.sin(rotation);
		transformationMatrix[3] = posX;
		transformationMatrix[4] = scaleX * (float) -Math.sin(rotation);
		transformationMatrix[5] = scaleY * (float) Math.cos(rotation);
		transformationMatrix[7] = posY;
		loadMatrix(uniform_transform, transformationMatrix);
	}
	
	public void loadViewMatrixIdentity() {
		loadMatrix(uniform_view, identityMatrix);
	}
	
	public void loadViewMatrix(boolean includeCamera) {
		aspectMatrix[0] = framebufferHeight/framebufferWidth;
		
		if(includeCamera) {
			loadCameraMatrix(cameraMatrix);
			clearMatrix(viewMatrix);
			multiply(aspectMatrix, cameraMatrix, viewMatrix);
			loadMatrix(uniform_view, viewMatrix);
		} else {
			loadMatrix(uniform_view, aspectMatrix);
		}
	}
	
	private static void clearMatrix(float[] matrix) {
		for(int i = 0; i < matrix.length; i++) {
			matrix[i] = 0;
		}
	}
	
	private void loadCameraMatrix(float[] matrix) {
		matrix[0] = camera.getZoom();
		matrix[3] = -camera.getPositionX() * camera.getZoom();
		matrix[5] = camera.getZoom();
		matrix[7] = -camera.getPositionY() * camera.getZoom();
	}
	
	public float getWorldViewportLeftBound() {
		float worldX = -framebufferWidth/framebufferHeight; //far left of screen after accounting for aspect ratio
		worldX /= camera.getZoom();
		worldX += camera.getPositionX();
		return worldX;
	}
	
	public float getWorldViewportRightBound() {
		float worldX = framebufferWidth/framebufferHeight; //far right of screen, after accounting for aspect ratio
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

	public Camera getCamera() {
		return camera;
	}
	
	public float getFramebufferWidth() {
		return framebufferWidth;
	}
	
	public float getFramebufferHeight() {
		return framebufferHeight;
	}
	
	public void setResolution(float framebufferWidth, float framebufferHeight) {
		this.framebufferWidth = framebufferWidth;
		this.framebufferHeight = framebufferHeight;
	}
	
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