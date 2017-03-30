package ritzow.solomon.engine.graphics;

public interface Renderer {
	
	/** 
	 * Renders an object, which can be continuously rendered by a GraphicsManager 
	 * @param framebufferWidth the width of the framebuffer to render
	 * @param framebufferHeight the height of the framebuffer to render
	 * @return A Framebuffer object the size of the screen that contains a Texture at color attachment 0 to be drawn
	 */
	public Framebuffer render(int framebufferWidth, int framebufferHeight);
}
