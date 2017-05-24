package ritzow.sandbox.client.graphics;

public final class ImmutableGraphics implements Graphics {
	protected final int model;
	protected final float opacity;
	protected final float scaleX;
	protected final float scaleY;
	protected final float rotation;
	
	public ImmutableGraphics(int modelIndex) {
		this(modelIndex, 1.0f, 1.0f, 1.0f, 0.0f);
	}
	
	public ImmutableGraphics(Graphics graphics, float opacity, float scaleX, float scaleY, float rotation) {
		this.model = graphics.getModelIndex();
		this.opacity = graphics.getOpacity() * opacity;
		this.scaleX = graphics.getScaleX() * scaleX;
		this.scaleY = graphics.getScaleY() * scaleY;
		this.rotation = graphics.getRotation() + rotation;
	}
	
	public ImmutableGraphics(int modelIndex, float opacity, float scaleX, float scaleY, float rotation) {
		this.model = modelIndex;
		this.opacity = opacity;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.rotation = rotation;
	}

	@Override
	public final int getModelIndex() {
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
