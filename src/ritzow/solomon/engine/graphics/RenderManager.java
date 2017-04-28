package ritzow.solomon.engine.graphics;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBlitFramebuffer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL;
import ritzow.solomon.engine.input.InputManager;
import ritzow.solomon.engine.input.handler.FramebufferSizeHandler;
import ritzow.solomon.engine.input.handler.WindowFocusHandler;
import ritzow.solomon.engine.util.Service;

/** Initializes OpenGL and loads data to the GPU, then renders any Renderable objects added to the List returned by getUpdatables() **/
public final class RenderManager implements Service, FramebufferSizeHandler, WindowFocusHandler {
	private volatile boolean setupComplete, exit, finished;
	private volatile boolean updateViewport, focused;
	private volatile int framebufferWidth, framebufferHeight;
	private final Display display;
	private final List<Renderer> renderers;
	private final Consumer<RenderManager> onStartup;
	
	public RenderManager(Display display) {
		this(display, a -> {});
	}
	
	public RenderManager(Display display, Consumer<RenderManager> onStartup) {
		this.display = display;
		this.link(display.getInputManager());
		this.renderers = new ArrayList<Renderer>();
		this.onStartup = onStartup;
	}

	@Override
	public void run() {
		display.setContext();
		GL.createCapabilities();
		glfwSwapInterval(0);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		try {
			Textures.loadAll(new File("resources/assets/textures"));
			Models.loadAll();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		onStartup.accept(this);
		
		try {
			GraphicsUtility.checkErrors();
		} catch(OpenGLException e) {
			e.printStackTrace();
		}

		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}
		
		try {
			while(!exit) {
				if(focused) {
					if(updateViewport) {
						glViewport(0, 0, framebufferWidth, framebufferHeight);
						updateViewport = false;
					}
					
					glClear(GL_COLOR_BUFFER_BIT );
					
					for(Renderer r : renderers) {
						Framebuffer result = r.render(framebufferWidth, framebufferHeight);
						glBindFramebuffer(GL_READ_FRAMEBUFFER, result.framebufferID);
						glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
						glBlitFramebuffer(0, 0, framebufferWidth, framebufferHeight, 0, 0, framebufferWidth, framebufferHeight, GL_COLOR_BUFFER_BIT, GL_LINEAR);
					}
				    
					try {
						GraphicsUtility.checkErrors();
					} catch(OpenGLException e) {
						e.printStackTrace();
					}
				    
					display.refresh();
					Thread.sleep(16);
				} else {
					synchronized(this) {
						//pauses rendering when window is not active to reduce idle CPU usage
						while(!(focused || exit)) {
							wait();
						}
					}
				}
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
		} finally {
			GraphicsUtility.checkErrors();
			renderers.clear();
			display.closeContext();
			GL.destroy();
			
			synchronized(this) {
				finished = true;
				notifyAll();
			}
		}
	}
	
	public List<Renderer> getRenderers() {
		return renderers;
	}

	@Override
	public boolean isSetupComplete() {
		return setupComplete;
	}
	
	@Override
	public boolean isFinished() {
		return finished;
	}
	
	@Override
	public synchronized void exit() {
		exit = true;
		notifyAll();
	}

	@Override
	public void framebufferSize(int width, int height) {
		framebufferWidth = width;
		framebufferHeight = height;
		updateViewport = true;
	}

	@Override
	public void windowFocus(boolean focused) {
		this.focused = focused;
		
		if(focused) {
			synchronized(this) {
				notifyAll();
			}
		}
	}

	@Override
	public void link(InputManager manager) {
		manager.getFramebufferSizeHandlers().add(this);
		manager.getWindowFocusHandlers().add(this);
	}

	@Override
	public void unlink(InputManager manager) {
		manager.getFramebufferSizeHandlers().remove(this);
		manager.getWindowFocusHandlers().remove(this);
	}
}
