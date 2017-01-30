package ritzow.solomon.engine.graphics;

public final class ImmutableGraphics implements Graphics {
	protected final Model model;
	protected final float opacity;
	protected final float scaleX;
	protected final float scaleY;
	protected final float rotation;
	
	public ImmutableGraphics(Model model) {
		this(model, 1.0f, 1.0f, 1.0f, 0.0f);
	}
	
	public ImmutableGraphics(Graphics graphics, float opacity, float scaleX, float scaleY, float rotation) {
		this.model = graphics.getModel();
		this.opacity = graphics.getOpacity() * opacity;
		this.scaleX = graphics.getScaleX() * scaleX;
		this.scaleY = graphics.getScaleY() * scaleY;
		this.rotation = graphics.getRotation() + rotation;
	}
	
	public ImmutableGraphics(Model model, float opacity, float scaleX, float scaleY, float rotation) {
		this.model = model;
		this.opacity = opacity;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.rotation = rotation;
	}
	
	@Override
	public void render(ModelRenderer renderer, float x, float y) {
		if(model != null) {
			renderer.loadOpacity(opacity);
			renderer.loadTransformationMatrix(x, y, scaleX, scaleY, rotation);
			model.render();
		}
	}

	@Override
	public void render(ModelRenderer renderer, float x, float y, float opacity, float scaleX, float scaleY, float rotation) {
		if(model != null) {
			renderer.loadOpacity(this.opacity * opacity);
			renderer.loadTransformationMatrix(x, y, this.scaleX * scaleX, this.scaleY * scaleY, this.rotation + rotation);
			model.render();
		}
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public float getOpacity() {
		return opacity;
	}

	@Override
	public float getScaleX() {
		return scaleX;
	}

	@Override
	public float getScaleY() {
		return scaleY;
	}

	@Override
	public float getRotation() {
		return rotation;
	}

}
