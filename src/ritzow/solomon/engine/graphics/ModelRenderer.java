package ritzow.solomon.engine.graphics;

import ritzow.solomon.engine.world.base.ModelRenderProgram;

public class ModelRenderer implements Renderer {
	protected final Model model;
	protected final ModelRenderProgram program;
	
	public ModelRenderer(Model model, ModelRenderProgram program) {
		this.model = model;
		this.program = program;
	}
	
	@Override
	public void render() {
		program.loadOpacity(1.0f);
		program.loadViewMatrixIdentity();
		
		if(program.getFramebufferWidth() > program.getFramebufferHeight()) {
			program.loadTransformationMatrix(0, 0, 2, 2 * program.getFramebufferWidth()/program.getFramebufferHeight(), 0);
		}
		
		else {
			program.loadTransformationMatrix(0, 0, 2 * program.getFramebufferHeight()/program.getFramebufferWidth(), 2, 0);
		}
		
		program.render(model);
	}

	@Override
	public void updateSize(int width, int height) {
		program.setResolution(width, height);
	}
}
