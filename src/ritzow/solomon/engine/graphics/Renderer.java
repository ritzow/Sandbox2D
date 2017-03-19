package ritzow.solomon.engine.graphics;

public interface Renderer {
	
	/** Renders an object, which can be continuously rendered by a GraphicsManager **/
	public void render();
	
	/** update the size of the renderer's framebuffer **/
	public void updateSize(int width, int height);
}
