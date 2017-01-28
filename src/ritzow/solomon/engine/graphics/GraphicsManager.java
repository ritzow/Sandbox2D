package ritzow.solomon.engine.graphics;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.lwjgl.opengl.GL;
import ritzow.solomon.engine.input.InputManager;
import ritzow.solomon.engine.input.handler.FramebufferSizeHandler;
import ritzow.solomon.engine.input.handler.WindowFocusHandler;
import ritzow.solomon.engine.resource.Fonts;
import ritzow.solomon.engine.resource.Models;
import ritzow.solomon.engine.resource.Textures;
import ritzow.solomon.engine.util.Exitable;
import ritzow.solomon.engine.util.Installable;
/**
 * Initializes OpenGL and loads data to the GPU, then renders any Renderable objects added to the List returned by getUpdatables()
 * @author Solomon Ritzow
 *
 */
public final class GraphicsManager implements Runnable, Installable, Exitable, FramebufferSizeHandler, WindowFocusHandler {
	private volatile boolean setupComplete, exit, finished;
	private volatile int framebufferWidth, framebufferHeight;
	private volatile boolean updateFrameSize;
	private volatile boolean focused;
	
	private ModelRenderer renderer;
	private final Display display;
	
	private final LinkedList<Renderable> renderables;
	
	public GraphicsManager(Display display) {
		this.display = display;
		this.renderables = new LinkedList<Renderable>();
		this.link(display.getInputManager());
	}

	@Override
	public void run() {
		display.setContext();
		GL.createCapabilities();
		glfwSwapInterval(0);
		glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		try {
			Textures.loadAll(new File("resources/assets/textures"));
			Models.loadAll();
			Fonts.loadAll(new File("resources/assets/fonts"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		renderer = new ModelRenderer(new Camera( 0, 0, 1));
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}
		
		try {
			while(!exit) {
				if(focused) {
					if(updateFrameSize) {
						glViewport(0, 0, framebufferWidth, framebufferHeight);
						renderer.setResolution(framebufferWidth, framebufferHeight);
						updateFrameSize = false;
					}
					
					glClear(GL_COLOR_BUFFER_BIT);
					
					for(Renderable r : renderables) {
						r.render(renderer);
					}
					
					display.refresh();
					Thread.sleep(1);
				}
				
				else {
					synchronized(this) {
						while(!focused && !exit) {
							this.wait();
						}
						updateFrameSize = true;
					}
				}
			}
		} catch(InterruptedException e) {
			System.err.println("Graphics Manager " + this.hashCode() + " was interrupted");
		} finally {
			renderables.clear();
			Models.deleteAll();
			display.closeContext();
			GL.destroy();
			
			synchronized(this) {
				finished = true;
				this.notifyAll();
			}
		}
	}
	
	public ModelRenderer getRenderer() {
		return renderer;
	}
	
	public List<Renderable> getRenderables() {
		return renderables;
	}
	
	public boolean isSetupComplete() {
		return setupComplete;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public synchronized void exit() {
		exit = true;
		this.notifyAll();
	}

	@Override
	public void framebufferSize(int width, int height) {
		framebufferWidth = width;
		framebufferHeight = height;
		updateFrameSize = true;
	}

	@Override
	public synchronized void windowFocus(boolean focused) {
		this.focused = focused;
		
		if(focused) {
			this.notifyAll();
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
