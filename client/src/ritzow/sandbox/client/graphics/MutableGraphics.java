package ritzow.sandbox.client.graphics;

public class MutableGraphics implements Graphics {
	public int model;
	public float opacity, rotation, scaleX, scaleY;
	
	public MutableGraphics(int modelIndex, float scaleX, float scaleY, float rotation, float opacity) {
		this.model = modelIndex;
		this.opacity = opacity;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.rotation = rotation;
	}

	@Override
	public final int getModelID() {
		return model;
	}

	@Override
	public final float getOpacity() {
		return opacity;
	}

	@Override
	public final float getScaleX() {
		return scaleX;
	}

	@Override
	public final float getScaleY() {
		return scaleY;
	}

	@Override
	public final float getRotation() {
		return rotation;
	}
}
