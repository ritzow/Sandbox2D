package ritzow.solomon.engine.graphics;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL;
import ritzow.solomon.engine.input.InputManager;
import ritzow.solomon.engine.input.handler.FramebufferSizeHandler;
import ritzow.solomon.engine.input.handler.WindowFocusHandler;
import ritzow.solomon.engine.util.Service;

/** Initializes OpenGL and loads data to the GPU, then renders any Renderable objects added to the List returned by getUpdatables() **/
public final class GraphicsUpdater implements Service, FramebufferSizeHandler, WindowFocusHandler {
	private volatile boolean setupComplete, exit, finished;
	private volatile boolean updateViewport, focused;
	private volatile int framebufferWidth, framebufferHeight;
	private final Display display;
	private final List<Renderer> renderers;
	private final Consumer<GraphicsUpdater>[] onStartup;
	
	@SafeVarargs
	public GraphicsUpdater(Display display, Consumer<GraphicsUpdater>... onStartup) {
		this.display = display;
		this.link(display.getInputManager());
		this.renderers = new LinkedList<Renderer>();
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
		
		for(Consumer<GraphicsUpdater> c : onStartup) {
			c.accept(this);
		}
		
		OpenGLException.checkErrors();

		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}
		
		glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		
		//explicitly create a framebuffer to render to (its contents will be copied to the visible framebuffer once the frame is complete)
		int displayBuffer = glGenFramebuffers();
		
		//create a texture to back the framebuffer object
		int colorBuffer = glGenTextures();
		
		//bind the framebuffer
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, displayBuffer);
		
		//specify the texture with width and height of 1 just because I have to
		glBindTexture(GL_TEXTURE_2D, colorBuffer);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, 1, 1, 0, GL_RGB, GL_FLOAT, 0);
		
		//attach the texture to the framebuffer
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorBuffer, 0);
		
		//make sure the framebuffer creation worked
		int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		
	    if (status != GL_FRAMEBUFFER_COMPLETE) {
	      	System.out.println("OpenGL Framebuffer Error: " + status);
	    }
		
		try {
			while(!exit) {
				if(focused) {
					if(updateViewport) {
						glViewport(0, 0, framebufferWidth, framebufferHeight);
						glBindTexture(GL_TEXTURE_2D, colorBuffer);
						glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, framebufferWidth, framebufferHeight, 0, GL_RGB, GL_FLOAT, 0);
						
						for(Renderer r : renderers) {
							r.updateSize(framebufferWidth, framebufferHeight);
						}
						
						updateViewport = false;
					}
					
					//bind the offscreen framebuffer for drawing
					glBindFramebuffer(GL_DRAW_FRAMEBUFFER, displayBuffer);
					glClear(GL_COLOR_BUFFER_BIT);
					
					//Run every runnable each frame
					for(Renderer r : renderers) { //TODO synchronize this (getRenderers can cause this to concurrentmod)
						r.render();
					}
					
					//bind offscreen framebuffer so we can read from it
					glBindFramebuffer(GL_READ_FRAMEBUFFER, displayBuffer);
					
					//bind onscreen framebuffer so we can write to it
					glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
					
					//copy the contents of the offscreen framebuffer to the onscreen framebuffer
				    glBlitFramebuffer(0, 0, framebufferWidth, framebufferHeight, 0, 0, framebufferWidth, framebufferHeight, GL_COLOR_BUFFER_BIT, GL_LINEAR);
				    
				    //bbind the onscreen framebuffer to both read and write (not sure if this really means anything here)
				    glBindFramebuffer(GL_FRAMEBUFFER, 0);
					
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
			OpenGLException.checkErrors();
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
