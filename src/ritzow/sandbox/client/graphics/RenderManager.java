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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.handler.FramebufferSizeHandler;
import ritzow.sandbox.client.input.handler.WindowFocusHandler;
import ritzow.sandbox.util.Service;

/** Initializes OpenGL and loads data to the GPU, then renders any Renderable objects added to the List returned by getUpdatables() **/
public final class RenderManager implements Service, FramebufferSizeHandler, WindowFocusHandler {
	private volatile boolean setupComplete, exit, finished;
	private volatile boolean updateViewport, focused;
	private volatile int framebufferWidth, framebufferHeight;
	private final Display display;
	private final List<Renderer> renderers;
	private final Queue<Consumer<RenderManager>> renderTasks;
	
	public RenderManager(Display display) {
		this(display, a -> {});
	}
	
	public RenderManager(Display display, Consumer<RenderManager> onStartup) {
		this.display = display;
		this.link(display.getInputManager());
		this.renderers = new ArrayList<Renderer>();
		this.renderTasks = new LinkedList<Consumer<RenderManager>>();
	}
	
	public void start() {
		new Thread(this, "Render Manager").start();
	}

	@Override
	public void run() {
		display.setContext();
		org.lwjgl.opengl.GL.createCapabilities();
		glfwSwapInterval(0);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		
		try {
			Textures.loadAll(new File("resources/assets/textures"));
			Models.loadAll();
			GraphicsUtility.checkErrors();
		} catch(IOException | OpenGLException e) {
			throw new RuntimeException(e);
		}

		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}
		
		try {
			while(!exit) {
				if(focused) {
					synchronized(renderTasks) {
						Consumer<RenderManager> task = renderTasks.poll();
						if(task != null) {
							task.accept(this);
						}
					}
					
					if(updateViewport) {
						glViewport(0, 0, framebufferWidth, framebufferHeight);
						updateViewport = false;
					}
					
					glClear(GL_COLOR_BUFFER_BIT);
					
					for(Renderer r : renderers) {
						glBindFramebuffer(GL_READ_FRAMEBUFFER, r.render(framebufferWidth, framebufferHeight).framebufferID);
						glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
						glBlitFramebuffer(0, 0, framebufferWidth, framebufferHeight, 0, 0, framebufferWidth, framebufferHeight, GL_COLOR_BUFFER_BIT, GL_LINEAR);
					}
					
					GraphicsUtility.checkErrors();
					display.refresh();
					Thread.sleep(16);
				} else {
					synchronized(this) {
						while(!(focused || exit)) {
							wait(); //pauses rendering when window is not active to reduce idle CPU usage
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
			org.lwjgl.opengl.GL.destroy();
			
			synchronized(this) {
				finished = true;
				notifyAll();
			}
		}
	}
	
	/** Will add a render task to a queue to be executed at the beginning of the next frame **/
	public void addRenderTask(Consumer<RenderManager> task) {
		synchronized(renderTasks) {
			renderTasks.add(task);
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
