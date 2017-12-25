package ritzow.sandbox.client.graphics;

public final class ImmutableGraphics implements Graphics {
	private final int model;
	private final float opacity, rotation, scaleX, scaleY;
	
	public ImmutableGraphics(int modelIndex, float opacity, float scaleX, float scaleY, float rotation) {
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
