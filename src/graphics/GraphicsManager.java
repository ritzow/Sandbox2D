package graphics;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL11.*;

import input.handler.FramebufferSizeHandler;
import input.handler.WindowIconifyHandler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.lwjgl.opengl.GL;
import resource.Models;
import resource.Textures;
import util.Exitable;
import util.Installable;

public final class GraphicsManager implements Runnable, Installable, Exitable, FramebufferSizeHandler, WindowIconifyHandler {
	private volatile boolean setupComplete;
	private volatile boolean exit;
	private volatile boolean finished;
	
	private volatile float framebufferWidth;
	private volatile float framebufferHeight;
	private volatile boolean updateFrameSize;
	private volatile boolean iconified;
	
	private Renderer renderer;
	private Display display;
	
	private final ArrayList<Renderable> renderables;
	
	public GraphicsManager(Display display) {
		this.display = display;
		this.renderables = new ArrayList<Renderable>();
		display.getInputManager().getFramebufferSizeHandlers().add(this);
		display.getInputManager().getWindowIconifyHandlers().add(this);
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
			Models.loadAll(new File("resources/assets/textures"));
			Models.loadDefaultFont(new File("resources/assets/fonts/default"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ShaderProgram shaderProgram = new ShaderProgram(
				new Shader("resources/shaders/vertexShader", org.lwjgl.opengl.GL20.GL_VERTEX_SHADER), 
				new Shader("resources/shaders/fragmentShader", org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER)
		);
		
		Camera camera = new Camera( 0, 0, 0.05f, 1.5f);
		renderer = new Renderer(shaderProgram, camera, display);
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}
		
		try {
			while(!exit) {
				if(!iconified) {
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
						while(iconified) {
							this.wait(); //TODO screen is white when de-iconified most of the time, chance of being white changes with time slept after waiting
						}
					}
				}
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
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
			updateFrameSize = true;
			this.notifyAll();
		}
	}
}
