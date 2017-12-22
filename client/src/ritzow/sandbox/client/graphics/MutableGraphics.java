package ritzow.sandbox.client.graphics;

public class MutableGraphics implements Graphics {
	protected int model;
	protected float opacity;
	protected float scaleX;
	protected float scaleY;
	protected float rotation;
	
	public MutableGraphics(int modelIndex) {
		this(modelIndex, 1.0f, 1.0f, 1.0f, 0.0f);
	}
	
	public MutableGraphics(Graphics graphics, float opacity, float scaleX, float scaleY, float rotation) {
		this.model = graphics.getModelID();
		this.opacity = graphics.getOpacity() * opacity;
		this.scaleX = graphics.getScaleX() * scaleX;
		this.scaleY = graphics.getScaleY() * scaleY;
		this.rotation = graphics.getRotation() + rotation;
	}
	
	public MutableGraphics(int modelIndex, float opacity, float scaleX, float scaleY, float rotation) {
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

	public final void setModel(int modelIndex) {
		this.model = modelIndex;
	}

	public final void setOpacity(float opacity) {
		this.opacity = opacity;
	}

	public final void setScaleX(float scaleX) {
		this.scaleX = scaleX;
	}

	public final void setScaleY(float scaleY) {
		this.scaleY = scaleY;
	}

	public final void setRotation(float rotation) {
		this.rotation = rotation;
	}
}
