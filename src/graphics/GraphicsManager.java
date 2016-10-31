package graphics;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL11.*;

import input.InputManager;
import input.handler.FramebufferSizeHandler;
import input.handler.WindowFocusHandler;
import input.handler.WindowIconifyHandler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.lwjgl.opengl.GL;
import resource.Models;
import resource.Shaders;
import resource.Textures;
import util.Exitable;
import util.Installable;

public final class GraphicsManager implements Runnable, Installable, Exitable, FramebufferSizeHandler, WindowIconifyHandler, WindowFocusHandler {
	private volatile boolean setupComplete;
	private volatile boolean exit;
	private volatile boolean finished;
	
	private volatile float framebufferWidth;
	private volatile float framebufferHeight;
	private volatile boolean updateFrameSize;
	private volatile boolean iconified;
	private volatile boolean focused;
	
	private Renderer renderer;
	private Display display;
	
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
			Shaders.loadAll(new File("resources/shaders"));
			Textures.loadAll(new File("resources/assets/textures"));
			Models.loadAll(new File("resources/assets/textures"));
			Models.loadDefaultFont(new File("resources/assets/fonts/default"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		renderer = new Renderer(new Camera( 0, 0, 1));
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}
		
		try {
			while(!exit) {
				if(!iconified && focused) {
					if(updateFrameSize) {
						glViewport(0, 0, (int)framebufferWidth, (int)framebufferHeight);
						renderer.setResolution((int)framebufferWidth, (int)framebufferHeight);
						updateFrameSize = false;
					}
					
					glClear(GL_COLOR_BUFFER_BIT);
					
					for(int i = 0; i < renderables.size(); i++) {
						renderables.get(i).render(renderer);
					}

					glFinish();
					display.refresh();
					Thread.sleep(1);
				}
				
				else {
					synchronized(this) {
						while((iconified || !focused) && !exit) {
							this.wait();
						}
						updateFrameSize = true;
					}
				}
			}
		} catch(InterruptedException e) {
			
		}

		
		renderables.clear();
		Models.deleteAll();
		display.closeContext();
		GL.destroy();
		
		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}
	
	public Renderer getRenderer() {
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
	
	public void exit() {
		exit = true;
	}

	@Override
	public synchronized void framebufferSize(int width, int height) {
		framebufferWidth = width;
		framebufferHeight = height;
		updateFrameSize = true;
	}

	@Override
	public synchronized void windowIconify(boolean iconified) {
		this.iconified = iconified;

		if(!iconified) {
			this.notifyAll();
		}
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
		manager.getWindowIconifyHandlers().add(this);
		manager.getWindowFocusHandlers().add(this);
	}

	@Override
	public void unlink(InputManager manager) {
		manager.getFramebufferSizeHandlers().remove(this);
		manager.getWindowIconifyHandlers().remove(this);
		manager.getWindowFocusHandlers().remove(this);
	}
}
