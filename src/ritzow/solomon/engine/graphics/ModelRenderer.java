package ritzow.solomon.engine.graphics;

import java.io.File;
import java.io.IOException;

public final class ModelRenderer extends ShaderProgram {
	private final Camera camera;
	private final int uniform_transform;
	private final int uniform_opacity;
	private final int uniform_view;
	
	//store models in ModelRenderer, rather than Models class
	private float framebufferWidth;
	private float framebufferHeight;
	
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
	
	public ModelRenderer(File vertexShader, File fragmentShader, Camera camera) throws IOException {
		super(new Shader(vertexShader, org.lwjgl.opengl.GL20.GL_VERTEX_SHADER), new Shader(fragmentShader, org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER));
		this.camera = camera;
		this.uniform_transform = getUniformID("transform");
		this.uniform_opacity = getUniformID("opacity");
		this.uniform_view = getUniformID("view");
	}
	
	public final void loadOpacity(float opacity) {
		loadFloat(uniform_opacity, opacity);
	}
	
	public final void renderModelIndex(int modelIndex) {
		Models.get(modelIndex).render();
	}
	
	public final void renderModel(int modelIndex, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		loadOpacity(opacity);
		loadTransformationMatrix(posX, posY, scaleX, scaleY, rotation);
		Models.get(modelIndex).render();
	}
	
	public final void renderGraphics(Graphics g, float posX, float posY) {
		renderModel(g.getModelIndex(), g.getOpacity(), posX, posY,  g.getScaleX(), g.getScaleY(), g.getRotation());
	}
	
	public final void renderGraphics(Graphics g, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		renderModel(g.getModelIndex(), g.getOpacity() * opacity, posX, posY, g.getScaleX() * scaleX, g.getScaleY() * scaleY, g.getRotation() + rotation);
	}
	
	public final void loadTransformationMatrix(float posX, float posY, float scaleX, float scaleY, float rotation) {
		transformationMatrix[0] = scaleX * (float) Math.cos(rotation);
		transformationMatrix[1] = scaleY * (float) Math.sin(rotation);
		transformationMatrix[3] = posX;
		transformationMatrix[4] = scaleX * (float) -Math.sin(rotation);
		transformationMatrix[5] = scaleY * (float) Math.cos(rotation);
		transformationMatrix[7] = posY;
		loadMatrix(uniform_transform, transformationMatrix);
	}
	
	public final void loadViewMatrixIdentity() {
		loadMatrix(uniform_view, identityMatrix);
	}
	
	public final void loadViewMatrix(boolean includeCamera) {
		aspectMatrix[0] = framebufferHeight/framebufferWidth;
		
		if(includeCamera) {
			loadCameraMatrix(cameraMatrix);
			clearMatrix(viewMatrix);
			multiply(aspectMatrix, cameraMatrix, viewMatrix);
			loadMatrix(uniform_view, viewMatrix);
		}
		
		else {
			loadMatrix(uniform_view, aspectMatrix);
		}
	}
	
	private final void clearMatrix(float[] matrix) {
		for(int i = 0; i < matrix.length; i++) {
			matrix[i] = 0;
		}
	}
	
	private final void loadCameraMatrix(float[] matrix) {
		matrix[0] = camera.getZoom();
		matrix[3] = -camera.getPositionX() * camera.getZoom();
		matrix[5] = camera.getZoom();
		matrix[7] = -camera.getPositionY() * camera.getZoom();
	}
	
	public final float getWorldViewportLeftBound() {
		float worldX = -framebufferWidth/framebufferHeight; //far left of screen after accounting for aspect ratio
		worldX /= camera.getZoom();
		worldX += camera.getPositionX();
		return worldX;
	}
	
	public final float getWorldViewportRightBound() {
		float worldX = framebufferWidth/framebufferHeight; //far right of screen, after accounting for aspect ratio
		worldX /= camera.getZoom();
		worldX += camera.getPositionX();
		return worldX;
	}
	
	public final float getWorldViewportTopBound() {
		float worldY = 1;
		worldY /= camera.getZoom();
		worldY += camera.getPositionY();
		return worldY;
	}
	
	public final float getWorldViewportBottomBound() {
		float worldY = -1;
		worldY /= camera.getZoom();
		worldY += camera.getPositionY();
		return worldY;
	}

	public final Camera getCamera() {
		return camera;
	}
	
	public final float getFramebufferWidth() {
		return framebufferWidth;
	}
	
	public final float getFramebufferHeight() {
		return framebufferHeight;
	}
	
	public final void setResolution(float framebufferWidth, float framebufferHeight) {
		this.framebufferWidth = framebufferWidth;
		this.framebufferHeight = framebufferHeight;
	}
	
	private static final void multiply(float[] a, float[] b, float[] destination) {
		for(int i = 0; i < 4; i++){
			for(int j = 0; j < 4; j++){
				for(int k = 0; k < 4; k++){
					set(destination, i, j, get(destination, i, j) + get(a, i, k) * get(b, k, j));
				}
			}
		}
	}
	
	private static final float get(float[] array, int row, int column) {
		return array[(row * 4) + column];
	}
	
	private static final void set(float[] array, int row, int column, float value) {
		array[(row * 4) + column] =  value;
	}
}