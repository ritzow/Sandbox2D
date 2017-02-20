package ritzow.solomon.engine.graphics;

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
		this.model = graphics.getModelIndex();
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

	public final int getModelIndex() {
		return model;
	}

	public final float getOpacity() {
		return opacity;
	}

	public final float getScaleX() {
		return scaleX;
	}

	public final float getScaleY() {
		return scaleY;
	}

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
