package graphics;

import static util.MatrixMath.multiply;

public final class ModelRenderer extends ShaderProgram {
	private volatile Camera camera;
	private int uniform_transform;
	private int uniform_opacity;
	private int uniform_view;
	
	private float framebufferWidth;
	private float framebufferHeight;
	
	private static float[] identityMatrix = new float[] {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};
	
	private float[] transformationMatrix = new float[] {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 0, 0,
			0, 0, 0, 1,
	};
	
	public ModelRenderer(Camera camera) {
		super(new Shader("resources/shaders/vertexShader", org.lwjgl.opengl.GL20.GL_VERTEX_SHADER), 
				new Shader("resources/shaders/fragmentShader", org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER));
		
		this.camera = camera;
		this.uniform_transform = getUniformID("transform");
		this.uniform_opacity = getUniformID("opacity");
		this.uniform_view = getUniformID("view");
	}
	
	public final void loadOpacity(float opacity) {
		loadFloat(uniform_opacity, opacity);
	}
	
	public final void loadTransformationMatrixIndentity() {
		loadMatrix(uniform_transform, identityMatrix);
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
		float[] view = new float[] {
				framebufferHeight/framebufferWidth, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 0, 0,
				0, 0, 0, 1,
		};
		
		if(includeCamera) {
			view = multiply(view, new float[] {
					camera.getZoom(), 0, 0, -camera.getPositionX() * camera.getZoom(),
					0, camera.getZoom(), 0, -camera.getPositionY() * camera.getZoom(),
					0, 0, 0, 0,
					0, 0, 0, 1,
			});
		}
		
		loadMatrix(uniform_view, view);
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