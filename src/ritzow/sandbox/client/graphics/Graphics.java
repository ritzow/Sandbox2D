package ritzow.sandbox.client.graphics;

public interface Graphics {
//	public void render(ModelRenderer renderer, float x, float y);
//	public void render(ModelRenderer renderer, float x, float y, float opacity, float scaleX, float scaleY, float rotation);
	public int getModelIndex();
	public float getOpacity();
	public float getScaleX();
	public float getScaleY();
	public float getRotation();
}