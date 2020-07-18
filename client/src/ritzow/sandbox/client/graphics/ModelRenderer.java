package ritzow.sandbox.client.graphics;

public interface ModelRenderer {
	void setCurrent();
	void loadIdentityMatrix();
	void loadViewMatrix(float ratio);
	void loadViewMatrixStandard(int framebufferWidth, int framebufferHeight);
	void loadViewMatrix(Camera camera, int framebufferWidth, int framebufferHeight);
	void queueRender(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation);
	void flush();
	void delete();
}
