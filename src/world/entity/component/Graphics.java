package world.entity.component;

import graphics.Model;
import graphics.ModelRenderer;
import java.io.Serializable;

public class Graphics implements Serializable {
	private static final long serialVersionUID = 4497437671840075738L;
	
	protected Model model;
	protected float opacity;
	protected float scaleX;
	protected float scaleY;
	protected float rotation;
	protected float rotationVelocity;
	
	public Graphics(Model model) {
		this(model, 1.0f, 1.0f, 1.0f, 0.0f);
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

	public final float getRotationVelocity() {
		return rotationVelocity;
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

	public final void setRotationVelocity(float rotationVelocity) {
		this.rotationVelocity = rotationVelocity;
	}
	
}
