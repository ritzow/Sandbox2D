package ritzow.sandbox.client.graphics;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBlitFramebuffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.lwjgl.glfw.GLFW;
import ritzow.sandbox.client.input.EventDelegator;
import ritzow.sandbox.client.input.handler.FramebufferSizeHandler;

/** Contains instance methods for initializing and destroying OpenGL, and rendering and updating the display**/
public final class RenderManager implements FramebufferSizeHandler {
	private volatile boolean updateViewport;
	private volatile int framebufferWidth, framebufferHeight;
	private final List<Renderer> renderers;
	
	public RenderManager(Display display) {
		this.link(display.getEventDelegator());
		this.renderers = new ArrayList<>();
	}
	
	@SuppressWarnings("static-method")
	public void initialize(Display display) {
		display.setGraphicsContextOnThread();
		org.lwjgl.opengl.GL.createCapabilities();
		glfwSwapInterval(0);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		GraphicsUtility.checkErrors();
	}

	public void close(Display display) {
		renderers.clear();
		GraphicsUtility.checkErrors();
		GLFW.glfwMakeContextCurrent(0);
		org.lwjgl.opengl.GL.destroy();
		unlink(display.getEventDelegator());
	}
	
	public void run(Display display) {
		int width = framebufferWidth, height = framebufferHeight;
		if(display.focused() && width > 0 && height > 0) {
			if(updateViewport) {
				glViewport(0, 0, width, height);
				updateViewport = false;
			}

			glClear(GL_COLOR_BUFFER_BIT);

			for(Renderer r : renderers) {
				glBindFramebuffer(GL_READ_FRAMEBUFFER, r.render(width, height).framebufferID);
				glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
				glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_LINEAR);
			}
			
			display.refresh();
			GraphicsUtility.checkErrors();
		}
	}
	
	public Collection<Renderer> getRenderers() {
		return renderers;
	}

	@Override
	public void framebufferSize(int width, int height) {
		framebufferWidth = width;
		framebufferHeight = height;
		updateViewport = true;
	}

	public void link(EventDelegator manager) {
		manager.framebufferSizeHandlers().add(this);
	}

	public void unlink(EventDelegator manager) {
		manager.framebufferSizeHandlers().remove(this);
	}
}
