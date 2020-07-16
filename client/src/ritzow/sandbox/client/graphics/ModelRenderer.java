package ritzow.sandbox.client.graphics;

public interface ModelRenderer {
	void setCurrent();
	void loadViewMatrixStandard(int framebufferWidth, int framebufferHeight);
	void loadViewMatrix(Camera camera, int framebufferWidth, int framebufferHeight);
	void queueRender(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation);
	void flush();
	void delete();
}
