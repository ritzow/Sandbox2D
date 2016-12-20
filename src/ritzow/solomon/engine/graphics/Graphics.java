package ritzow.solomon.engine.graphics;

public abstract class Graphics {
	public abstract void render(ModelRenderer renderer, float x, float y);
	public abstract void render(ModelRenderer renderer, float x, float y, float opacity, float scaleX, float scaleY, float rotation);
	public abstract Model getModel();
	public abstract float getOpacity();
	public abstract float getScaleX();
	public abstract float getScaleY();
	public abstract float getRotation();
}
