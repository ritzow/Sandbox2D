package ritzow.sandbox.client.graphics;

public final class MenuRenderer implements Renderer {
	private final ModelRenderProgram modelProgram;
	private final Camera camera;

	public MenuRenderer(ModelRenderProgram modelProgram) {
		this.modelProgram = modelProgram;
		this.camera = new Camera(0, 0, 1.0f);
		GraphicsUtility.checkErrors();
	}

	@Override
	public void render(Framebuffer dest, final int width, final int height) {
		//prepare the diffuse texture for drawing
		dest.clear(0.0f, 0.0f, 1.0f, 1.0f);
		dest.setDraw();
		
		//ensure that model program is cached on stack
		ModelRenderProgram program = this.modelProgram;
		
		//set the current shader program
		program.setCurrent();

		//load the view transformation
		program.loadViewMatrix(camera, width, height);

		program.render(RenderConstants.MODEL_GREEN_FACE, 1.0f, 0, 0, 1.0f, 1.0f, 0.0f);
	}
}