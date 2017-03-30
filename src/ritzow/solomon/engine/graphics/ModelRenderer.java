package ritzow.solomon.engine.graphics;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import ritzow.solomon.engine.world.base.ModelRenderProgram;

public class ModelRenderer implements Renderer {
	protected final Model model;
	protected final ModelRenderProgram program;
	protected final Framebuffer framebuffer;
	protected final int diffuseTexture;
	
	private int previousWidth, previousHeight;
	
	public ModelRenderer(Model model, ModelRenderProgram program) {
		this.model = model;
		this.program = program;
		this.framebuffer = new Framebuffer();
		this.diffuseTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, diffuseTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 16, 16, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
		framebuffer.attachTexture(diffuseTexture, GL_COLOR_ATTACHMENT0);
		GraphicsUtility.checkErrors();
	}
	
	@Override
	public Framebuffer render(int framebufferWidth, int framebufferHeight) {
		program.setCurrent();
		if(previousWidth != framebufferWidth || previousHeight != framebufferHeight) {
			program.setResolution(framebufferWidth, framebufferHeight);
			glBindTexture(GL_TEXTURE_2D, diffuseTexture);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, framebufferWidth, framebufferHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
			previousWidth = framebufferWidth;
			previousHeight = framebufferHeight;
		}
		program.loadOpacity(1.0f);
		program.loadViewMatrixIdentity();
		
		//TODO doesn't fill screen in all cases (high height to width ratios)
		float frameWidth = program.getFramebufferWidth();
		float frameHeight = program.getFramebufferHeight();
		float scaleX = frameWidth > frameHeight ? 2 : 2 * frameWidth/frameHeight;
		float scaleY = frameWidth > frameHeight ? 2 * frameWidth/frameHeight : 2;
		program.loadTransformationMatrix(0, 0, scaleX, scaleY, 0);
		
		framebuffer.setDraw();
		program.render(model);
		
		return framebuffer;
	}
}
