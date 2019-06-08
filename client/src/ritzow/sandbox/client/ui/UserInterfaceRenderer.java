package ritzow.sandbox.client.ui;

import java.util.ArrayList;
import java.util.Collection;
import ritzow.sandbox.client.graphics.Framebuffer;
import ritzow.sandbox.client.graphics.ModelRenderProgram;
import ritzow.sandbox.client.graphics.OpenGLException;
import ritzow.sandbox.client.graphics.OpenGLTexture;
import ritzow.sandbox.client.graphics.Renderer;

import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

public class UserInterfaceRenderer implements Renderer {
	private final Collection<ElementEntry> elements;
	private final ModelRenderProgram program;
	private final Framebuffer buffer;
	
	private static final class ElementEntry {
		float posX, posY;
		UIElement element;
		
		ElementEntry(float posX, float posY, UIElement element) {
			this.posX = posX;
			this.posY = posY;
			this.element = element;
		}
	}
	
	public UserInterfaceRenderer(ModelRenderProgram program) {
		this.program = program;
		elements = new ArrayList<ElementEntry>();
		buffer = new Framebuffer();
		buffer.attachTexture(new OpenGLTexture(100, 100), GL_COLOR_ATTACHMENT0);
	}

	@Override
	public void render(Framebuffer dest, int framebufferWidth, int framebufferHeight) throws OpenGLException {
		//also update the elements in here
		for(ElementEntry entry : elements) {
			program.render(entry.element, entry.posX, entry.posY);
		}
	}
	
	public void add(UIElement element, float posX, float posY) {
		elements.add(new ElementEntry(posX, posY, element));
	}
}
