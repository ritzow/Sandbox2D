package ritzow.solomon.engine.graphics;

public class Background implements Renderable {
	protected final Model model;
	
	public Background(Model model) {
		this.model = model;
	}
	
	@Override
	public void render(ModelRenderer renderer) {
		renderer.loadOpacity(1.0f);
		renderer.loadViewMatrixIdentity();
		
		if(renderer.getFramebufferWidth() > renderer.getFramebufferHeight()) {
			renderer.loadTransformationMatrix(0, 0, 2, 2 * renderer.getFramebufferWidth()/renderer.getFramebufferHeight(), 0);
		}
		
		else {
			renderer.loadTransformationMatrix(0, 0, 2 * renderer.getFramebufferHeight()/renderer.getFramebufferWidth(), 2, 0);
		}
		
		model.render();
	}
	
}
