package graphics;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL11.*;

import input.handler.FramebufferSizeHandler;
import java.util.ArrayList;
import main.Exitable;
import main.Installable;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import util.ResourceManager;

public final class GraphicsManager implements Runnable, Installable, Exitable, FramebufferSizeHandler {
	private volatile boolean setupComplete;
	private volatile boolean exit;
	private volatile boolean finished;
	
	private Renderer renderer;
	private Display display;
	
	private float frameWidth;
	private float frameHeight;
	
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
		
		ResourceManager.loadResources("resources/assets/");
		
		ShaderProgram shaderProgram = new ShaderProgram( //create the shader program
				new Shader("resources/shaders/vertexShader", org.lwjgl.opengl.GL20.GL_VERTEX_SHADER), 
				new Shader("resources/shaders/fragmentShader", org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER)
		);
		
		Camera camera = new Camera( 0, 0, 0.07f, 1.5f);
		renderer = new Renderer(shaderProgram, camera, display);
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}
		
		try {			
			while(!exit) {
				glViewport(0, 0, (int)frameWidth, (int)frameHeight);
				renderer.setResolution((int)frameWidth, (int)frameHeight);
				glClear(GL_COLOR_BUFFER_BIT);
				for(int i = 0; i < renderables.size(); i++) renderables.get(i).render(renderer);
				glFinish(); //wait until rendering is complete
				display.refresh();
				Thread.sleep(1);
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		renderables.clear();
		ResourceManager.deleteAll();
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
	}
}
