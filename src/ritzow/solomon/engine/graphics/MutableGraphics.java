package ritzow.solomon.engine.graphics;

public class MutableGraphics extends Graphics {
	protected Model model;
	protected float opacity;
	protected float scaleX;
	protected float scaleY;
	protected float rotation;
	
	public MutableGraphics(Model model) {
		this(model, 1.0f, 1.0f, 1.0f, 0.0f);
	}
	
	public MutableGraphics(Graphics graphics, float opacity, float scaleX, float scaleY, float rotation) {
		this.model = graphics.getModel();
		this.opacity = graphics.getOpacity() * opacity;
		this.scaleX = graphics.getScaleX() * scaleX;
		this.scaleY = graphics.getScaleY() * scaleY;
		this.rotation = graphics.getRotation() + rotation;
	}
	
	public MutableGraphics(Model model, float opacity, float scaleX, float scaleY, float rotation) {
		this.model = model;
		this.opacity = opacity;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.rotation = rotation;
	}
	
	public void render(ModelRenderer renderer, float x, float y) {
		if(model != null) {
			renderer.loadOpacity(opacity);
			renderer.loadTransformationMatrix(x, y, scaleX, scaleY, rotation);
			model.render();
		}
	}
	
	public void render(ModelRenderer renderer, float x, float y, float opacity, float scaleX, float scaleY, float rotation) {
		if(model != null) {
			renderer.loadOpacity(this.opacity * opacity);
			renderer.loadTransformationMatrix(x, y, this.scaleX * scaleX, this.scaleY * scaleY, this.rotation + rotation);
			model.render();
		}
	}

	public final Model getModel() {
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

	public final void setModel(Model model) {
		this.model = model;
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
