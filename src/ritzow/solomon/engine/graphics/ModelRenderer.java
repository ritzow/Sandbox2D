package ritzow.solomon.engine.graphics;

import static ritzow.solomon.engine.util.Utility.Matrix.multiply;

public final class ModelRenderer extends ShaderProgram {
	private volatile Camera camera;
	private int uniform_transform;
	private int uniform_opacity;
	private int uniform_view;
	
	private float framebufferWidth;
	private float framebufferHeight;
	
	private static final float[] identityMatrix = new float[] {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};
	
	private final float[] aspectMatrix = new float[] {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};
	
	private final float[] cameraMatrix = new float[] {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};
	
	private final float[] viewMatrix = new float[] {
			0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 0,
	};
	
	private final float[] transformationMatrix = new float[] {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};
	
	public ModelRenderer(Camera camera) {
		super(
			new Shader("resources/shaders/vertexShader", org.lwjgl.opengl.GL20.GL_VERTEX_SHADER),
			new Shader("resources/shaders/fragmentShader", org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER)
		);
		
		this.camera = camera;
		this.uniform_transform = getUniformID("transform");
		this.uniform_opacity = getUniformID("opacity");
		this.uniform_view = getUniformID("view");
	}
	
	public final void loadOpacity(float opacity) {
		loadFloat(uniform_opacity, opacity);
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

	public final void setCamera(Camera camera) {
		this.camera = camera;
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
}