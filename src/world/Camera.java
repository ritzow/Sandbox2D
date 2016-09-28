package world;

public class Camera {
	protected float posX;
	protected float posY;
	protected float zoom;
	protected float minZoom;
	protected float maxZoom;
	protected float velocityZ;
	
	public Camera(float posX, float posY, float minZoom, float maxZoom) {
		this.posX = posX;
		this.posY = posY;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
		resetZoom();
	}
	
	public void resetZoom() {
		this.zoom = minZoom + maxZoom * 0.1f;
	}
	
	public void update() {
		setZoom(zoom + zoom * velocityZ);
	}
	
	public final float getMinZoom() {
		return minZoom;
	}

	public final float getMaxZoom() {
		return maxZoom;
	}
	
	public final float getVelocityZ() {
		return velocityZ;
	}
	
	public final float getZoom() {
		return zoom;
	}
	
	public final void setVelocityZ(float velocityZ) {
		this.velocityZ = velocityZ;
	}
	
	public final void setMinZoom(float minZoom) {
		this.minZoom = minZoom;
	}

	public final void setMaxZoom(float maxZoom) {
		this.maxZoom = maxZoom;
	}
	
	public final void setZoom(float zoom) {
		this.zoom = Math.max(Math.min(zoom, maxZoom), minZoom);
	}

	public float getX() {
		return posX;
	}

	public float getY() {
		return posY;
	}

	public void setX(float x) {
		posX = x;
	}

	public void setY(float y) {
		posY = y;
	}
}