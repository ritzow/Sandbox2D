package graphics;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL11.*;

import input.handler.FramebufferSizeHandler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import resource.Models;
import resource.Textures;
import util.Exitable;
import util.Installable;

public final class GraphicsManager implements Runnable, Installable, Exitable, FramebufferSizeHandler {
	private volatile boolean setupComplete;
	private volatile boolean exit;
	private volatile boolean finished;
	
	private Renderer renderer;
	private Display display;
	
	private float frameWidth;
	private float frameHeight;
	private boolean updateFrame;
	
	private final ArrayList<Renderable> renderables;
	
	public GraphicsManager(Display display) {
		this.display = display;
		this.renderables = new ArrayList<Renderable>();
		display.getInputManager().getFramebufferSizeHandlers().add(this);
	}

	@Override
	public void run() {
		display.setContext();
		GL.createCapabilities();
		glfwSwapInterval(0);
		glClearColor(1.0f,1.0f,1.0f,1.0f);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		GLFWErrorCallback.createPrint(System.err).set();
		
		try {
			Textures.loadAll(new File("resources/assets/textures"));
			Models.loadAll(new File("resources/assets/textures"));
			Models.loadDefaultFont(new File("resources/assets/fonts/default"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ShaderProgram shaderProgram = new ShaderProgram( //create the shader program
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
				if(updateFrame) {
					glViewport(0, 0, (int)frameWidth, (int)frameHeight);
					renderer.setResolution((int)frameWidth, (int)frameHeight);
					updateFrame = false;
				}
				
				glClear(GL_COLOR_BUFFER_BIT);
				
				for(int i = 0; i < renderables.size(); i++) {
					renderables.get(i).render(renderer);
				}
				
				glFinish();
				display.refresh();
				Thread.sleep(1);
			}
		} catch(InterruptedException e) {
			
		}

		
		renderables.clear();
		GL.destroy();
		display.closeContext();
		
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
	public void framebufferSize(int width, int height) {
		frameWidth = width;
		frameHeight = height;
		updateFrame = true;
	}
}
