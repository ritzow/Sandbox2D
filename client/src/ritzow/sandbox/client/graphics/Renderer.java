package ritzow.sandbox.client.graphics;

public interface Renderer {
	
	/** 
	 * Renders an object, which can be continuously rendered by a GraphicsManager 
	 * @param dest The framebuffer that should be written to
	 * @param framebufferWidth the width of the framebuffer to render
	 * @param framebufferHeight the height of the framebuffer to render
	 * @throws OpenGLException if an OpenGL error occurred
	 */
	public void render(Framebuffer dest, int framebufferWidth, int framebufferHeight) throws OpenGLException;
}
