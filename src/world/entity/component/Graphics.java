package world.entity.component;

import graphics.Model;
import graphics.ModelRenderer;

public class Graphics {
	protected Model model;
	protected float opacity;
	protected float scaleX;
	protected float scaleY;
	protected float rotation;
	
	public Graphics(Model model) {
		this(model, 1.0f, 1.0f, 1.0f, 0.0f);
	}
	
	public Graphics(Graphics graphics, float opacity, float scaleX, float scaleY, float rotation) {
		this.model = graphics.model;
		this.opacity = graphics.opacity * opacity;
		this.scaleX = graphics.scaleX * scaleX;
		this.scaleY = graphics.scaleY * scaleY;
		this.rotation = graphics.rotation + rotation;
	}
	
	public Graphics(Model model, float opacity, float scaleX, float scaleY, float rotation) {
		this.model = model;
		this.opacity = opacity;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.rotation = rotation;
	}
	
	public void render(ModelRenderer renderer, float x, float y) {
		renderer.loadOpacity(opacity);
		renderer.loadTransformationMatrix(x, y, scaleX, scaleY, rotation);
		model.render();
	}
	
	public void render(ModelRenderer renderer, float x, float y, float opacity, float scaleX, float scaleY, float rotation) {
		renderer.loadOpacity(this.opacity * opacity);
		renderer.loadTransformationMatrix(x, y, this.scaleX * scaleX, this.scaleY * scaleY, this.rotation + rotation);
		model.render();
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
