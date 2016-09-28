package main;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glViewport;

import graphics.*;
import input.handler.FramebufferSizeHandler;
import java.util.ArrayList;
import org.lwjgl.glfw.GLFWErrorCallback;
import util.ResourceManager;
import world.Camera;

public final class GraphicsManager implements Runnable, Installable, Exitable, FramebufferSizeHandler {
	private volatile boolean setupComplete;
	private volatile boolean exit;
	private volatile boolean finished;
	
	private Renderer renderer;
	private Camera camera;
	private ShaderProgram shaderProgram;
	private Display display;
	
	private float frameWidth;
	private float frameHeight;
	
	private final ArrayList<Renderable> renderables;
	
	public GraphicsManager(Display display) {
		this.renderables = new ArrayList<Renderable>();
		this.display = display;
		display.getInputManager().getFramebufferSizeHandlers().add(this);
	}

	@Override
	public void run() {
		setup();
		loop();
		
		renderables.clear();
		ResourceManager.deleteAll();
		display.destroyContext();
		
		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}
	
	private void setup() {
		display.setupContext();
		GLFWErrorCallback.createPrint(System.err).set();
		
		ResourceManager.loadResources("resources/assets/"); //create "Models" and buffer data to GPU
		
		shaderProgram = new ShaderProgram( //create the shader program
				new Shader("resources/shaders/vertexShader", org.lwjgl.opengl.GL20.GL_VERTEX_SHADER), 
				new Shader("resources/shaders/fragmentShader", org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER)
		);
		
		camera = new Camera( 0, 0, 0.07f, 1.5f);
		renderer = new Renderer(shaderProgram, camera, display);
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}
	}
	
	private void loop() {
		try {			
			while(!exit) {
				glViewport(0, 0, (int)frameWidth, (int)frameHeight);
				glClear(GL_COLOR_BUFFER_BIT);
				
				for(int i = 0; i < renderables.size(); i++) {
					renderables.get(i).render(renderer);
				}
				
				display.refresh();
				Thread.sleep(1);
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public Renderer getRenderer() {
		return renderer;
	}
	
	public ArrayList<Renderable> getRenderables() {
		return renderables;
	}
	
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}
	
	public void setDisplay(Display display) {
		this.display = display;
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
