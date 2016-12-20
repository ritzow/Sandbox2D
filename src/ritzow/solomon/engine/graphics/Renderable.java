package ritzow.solomon.engine.graphics;

public interface Renderable {
	/**
	 * Renders an object, which can be continuously rendered by a GraphicsManager.
	 * @param renderer The ModelRenderer to render the object with.
	 */
	public void render(ModelRenderer renderer);
}
