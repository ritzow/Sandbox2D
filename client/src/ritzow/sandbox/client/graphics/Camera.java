package ritzow.sandbox.client.graphics;

public class Camera {
	protected float positionX;
	protected float positionY;
	protected float zoom;
	
	public Camera(float positionX, float positionY, float zoom) {
		this.positionX = positionX;
		this.positionY = positionY;
		this.zoom = zoom;
	}

	public final float getPositionX() {
		return positionX;
	}

	public final float getPositionY() {
		return positionY;
	}

	public final float getZoom() {
		return zoom;
	}

	public final void setPositionX(float positionX) {
		this.positionX = positionX;
	}

	public final void setPositionY(float positionY) {
		this.positionY = positionY;
	}

	public final void setZoom(float zoom) {
		this.zoom = zoom;
	}
}