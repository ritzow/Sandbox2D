package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL20C.glDrawBuffers;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;

public final class MenuRenderer implements Renderer {
	private final ModelRenderProgram modelProgram;
	private final Framebuffer framebuffer;
	private final OpenGLTexture diffuseTexture;
	private final Camera camera;
	private int previousWidth, previousHeight;

	public MenuRenderer(ModelRenderProgram modelProgram) {
		this.modelProgram = modelProgram;
		this.framebuffer = new Framebuffer();
		this.diffuseTexture = new OpenGLTexture(100, 100);
		framebuffer.attachTexture(diffuseTexture, GL_COLOR_ATTACHMENT0);
		GraphicsUtility.checkErrors();
		this.camera = new Camera(0, 0, 1.0f);
	}

	@Override
	public Framebuffer render(final int currentWidth, final int currentHeight) {
		//ensure that model program is cached on stack
		ModelRenderProgram program = this.modelProgram;

		//update framebuffer size
		if(previousWidth != currentWidth || previousHeight != currentHeight) {
			program.setResolution(currentWidth, currentHeight);
			diffuseTexture.setSize(currentWidth, currentHeight);
			previousWidth = currentWidth;
			previousHeight = currentHeight;
		}

		//set the current shader program
		program.setCurrent();

		//load the view transformation
		program.loadViewMatrix(camera);

		//prepare the diffuse texture for drawing
		framebuffer.clear(1.0f, 1.0f, 1.0f, 1.0f);
		framebuffer.setDraw();

		//tell the framebuffer to draw the shader output to attachment 0
		glDrawBuffers(GL_COLOR_ATTACHMENT0);

		//set the blending mode to allow transparency
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		int width = program.getFrameBufferWidth();
		int height = program.getFrameBufferHeight();
		
		program.render(RenderConstants.MODEL_GREEN_FACE, 1.0f, 0, 0, 1.0f, 1.0f, 0.0f);

		return framebuffer;
	}
}