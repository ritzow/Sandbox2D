package graphics;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL11.*;

import input.InputManager;
import input.handler.FramebufferSizeHandler;
import input.handler.WindowFocusHandler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.lwjgl.opengl.GL;
import resource.Fonts;
import resource.Models;
import resource.Textures;
import util.Exitable;
import util.Installable;
/**
 * Initializes OpenGL and loads data to the GPU, then render objects added to the renderables ArrayList.
 * @author Solomon Ritzow
 *
 */
public final class GraphicsManager implements Runnable, Installable, Exitable, FramebufferSizeHandler, WindowFocusHandler {
	private volatile boolean setupComplete, exit, finished;
	private volatile float framebufferWidth, framebufferHeight;
	private volatile boolean updateFrameSize;
	private volatile boolean focused;
	
	private ModelRenderer renderer;
	private final Display display;
	
	private final ArrayList<Renderable> renderables;
	
	public GraphicsManager(Display display) {
		this.display = display;
		this.renderables = new ArrayList<Renderable>();
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
			//Shaders.loadAll(new File("resources/shaders"));
			Textures.loadAll(new File("resources/assets/textures"));
			Models.loadAll(new File("resources/assets/textures"));
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
			while(!exit) { //TODO remove as much synchronization stuff as possible (all the volatile fields)
				if(focused) {
					if(updateFrameSize) {
						glViewport(0, 0, (int)framebufferWidth, (int)framebufferHeight);
						renderer.setResolution((int)framebufferWidth, (int)framebufferHeight);
						updateFrameSize = false;
					}
					
					glClear(GL_COLOR_BUFFER_BIT);
					
					for(Renderable r : renderables) {
						r.render(renderer);
					}
					
					glFinish();
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
	
	public ArrayList<Renderable> getRenderables() {
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
