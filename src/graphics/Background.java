package graphics;

public class Background implements Renderable {
	
	protected Model model;
	
	public Background(Model model) {
		this.model = model;
	}
	
	@Override
	public void render(Renderer renderer) {
		renderer.loadViewMatrixIdentity();
		renderer.loadTransformationMatrix(0, 0, 2, 2 * renderer.getFramebufferWidth()/renderer.getFramebufferHeight(), 0);
		model.render();
	}
}
