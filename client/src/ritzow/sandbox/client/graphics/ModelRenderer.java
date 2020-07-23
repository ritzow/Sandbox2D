package ritzow.sandbox.client.graphics;

public interface ModelRenderer {
	void setCurrent();
	void loadViewMatrixIdentity();
	void loadViewMatrixScale(float scale, int framebufferWidth, int framebufferHeight);
	void loadViewMatrixStandard(int framebufferWidth, int framebufferHeight);
	void loadViewMatrix(Camera camera, int framebufferWidth, int framebufferHeight);
	void queueRender(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation);
	void queueRender(Model model, float opacity, float exposure, float posX, float posY, float scaleX, float scaleY, float rotation);
	void flush();
	void delete();
	//TODO add method to get the rectangle bounds of each model (width and height, computed from vertices)
}
