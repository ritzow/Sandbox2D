package graphics;

import static util.MatrixMath.multiply;

public final class Renderer {
	private ShaderProgram program;
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
	
	public Renderer(ShaderProgram program, Camera camera, Display display) {	
		this.program = program;
		this.camera = camera;
		this.uniform_transform = program.getUniformID("transform");
		this.uniform_opacity = program.getUniformID("opacity");
		this.uniform_view = program.getUniformID("view");
	}
	
	public final void loadOpacity(float opacity) {
		program.loadFloat(uniform_opacity, opacity);
	}
	
	public final void loadTransformationMatrixIndentity() {
		program.loadMatrix(uniform_transform, identityMatrix);
	}
	
	public final void loadTransformationMatrix(float posX, float posY, float scaleX, float scaleY, float rotation) {
		program.loadMatrix(uniform_transform, 
			new float[] {
				scaleX * (float) Math.cos(rotation), scaleY * (float) Math.sin(rotation), 0, posX,
				scaleX * (float) -Math.sin(rotation), scaleY * (float) Math.cos(rotation), 0, posY,
				0, 0, 0, 0,
				0, 0, 0, 1,
			}
		);
	}
	
	public final void loadViewMatrixIdentity() {
		program.loadMatrix(uniform_view, identityMatrix);
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
					camera.getZoom(), 0, 0, -camera.getX() * camera.getZoom(),
					0, camera.getZoom(), 0, -camera.getY() * camera.getZoom(),
					0, 0, 0, 0,
					0, 0, 0, 1,
			});
		}
		
		program.loadMatrix(uniform_view, view);
	}
	
	public final float getWorldViewportLeftBound() {
		float worldX = -framebufferWidth/framebufferHeight; //far left of screen after accounting for aspect ratio
		worldX /= camera.getZoom();
		worldX += camera.getX();
		return worldX;
	}
	
	public final float getWorldViewportRightBound() {
		float worldX = framebufferWidth/framebufferHeight; //far right of screen, after accounting for aspect ratio
		worldX /= camera.getZoom();
		worldX += camera.getX();
		return worldX;
	}
	
	public final float getWorldViewportTopBound() {
		float worldY = 1;
		worldY /= camera.getZoom();
		worldY += camera.getY();
		return worldY;
	}
	
	public final float getWorldViewportBottomBound() {
		float worldY = -1;
		worldY /= camera.getZoom();
		worldY += camera.getY();
		return worldY;
	}

	public final ShaderProgram getProgram() {
		return program;
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