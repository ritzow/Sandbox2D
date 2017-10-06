package ritzow.sandbox.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import ritzow.sandbox.client.Client;
import ritzow.sandbox.client.audio.ClientAudioSystem;
import ritzow.sandbox.client.audio.Sounds;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.graphics.ClientWorldRenderer;
import ritzow.sandbox.client.graphics.LightRenderProgram;
import ritzow.sandbox.client.graphics.ModelRenderProgram;
import ritzow.sandbox.client.graphics.OpenGLException;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.graphics.Shader;
import ritzow.sandbox.client.input.Controls;
import ritzow.sandbox.client.input.EventProcessor;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.controller.CameraController;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.PlayerController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.input.handler.WindowCloseHandler;
import ritzow.sandbox.client.world.ClientWorld;
import ritzow.sandbox.util.RunnableRepeatExecutor;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class StartClient {
	public static void main(String... args) throws SocketException, UnknownHostException {
		SocketAddress serverAddress = new InetSocketAddress(args.length > 0 ? args[0] : InetAddress.getLocalHost().getHostAddress(), 50000);
		Client client = new Client(new InetSocketAddress(0)); //wildcard address, and any port
		client.start();
		
		System.out.print("Connecting to " + serverAddress + "... ");
		
		//attempt to connect to the server for one second, if the client connects, run the game code and event processor
		if(client.connectTo(serverAddress, 1000)) {
			System.out.println("connected!");
			EventProcessor eventProcessor = new EventProcessor();
			new Thread(new ClientStarter(client, eventProcessor), "Client Manager").start();
			eventProcessor.run(); //run the event processor on this thread
		} else {
			System.out.println("failed to connect.");
			client.stop();
		}
	}
	
	private static final class ClientStarter implements Runnable, WindowCloseHandler, KeyHandler {
		private final Client client;
		private final EventProcessor eventProcessor;
		private volatile boolean exit;
		
		public ClientStarter(Client client, EventProcessor eventManager) {
			this.client = client;
			this.eventProcessor = eventManager;
		}
		
		@Override
		public void run() {
			
			//wait for the client to receive the world and return it
			ClientWorld world = client.getWorld();
			System.out.println("world received");
			
			ClientAudioSystem audio = new ClientAudioSystem();
			
			try {
				//TODO make sound register system, something like AudioSystem::registerSound method 
				//to associate sounds with a system, rather than being global

				Sounds.loadAll(new File("resources/assets/audio"));
				world.setAudioSystem(audio);
				audio.setVolume(1.0f);
				System.out.println("audio system initialized");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			PlayerEntity player = client.getPlayer();
			System.out.println("player received");
			
			CameraController cameraGrip = new TrackingCameraController(new Camera(0, 0, 1), audio, player, 0.005f, 0.05f, 0.6f);
			
			eventProcessor.waitForSetup();
			
			RenderManager renderManager = eventProcessor.getDisplay().getRenderManager();
			
			//perform graphics setup
			renderManager.submitRenderTask(graphics -> {
				try {
					//load shader programs on renderer startup so that it is done in OpenGL thread, otherwise the program will crash
					ModelRenderProgram modelProgram = new ModelRenderProgram(
							new Shader(new FileInputStream("resources/shaders/modelVertexShader"), org.lwjgl.opengl.GL20.GL_VERTEX_SHADER),
							new Shader(new FileInputStream("resources/shaders/modelFragmentShader"), org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER), 
							cameraGrip.getCamera()
					);
					
					LightRenderProgram lightProgram = new LightRenderProgram(
							new Shader(new FileInputStream("resources/shaders/lightVertexShader"), org.lwjgl.opengl.GL20.GL_VERTEX_SHADER),
							new Shader(new FileInputStream("resources/shaders/lightFragmentShader"), org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER),
							cameraGrip.getCamera()
					);
					
					graphics.getRenderers().add(new ClientWorldRenderer(modelProgram, lightProgram, world));
					
					System.out.println("renderer shaders initialized");
				} catch (IOException | OpenGLException e) {
					throw new RuntimeException(e);
				}
			});
			
			renderManager.start();
			renderManager.waitForSetup();
			
			System.out.println("render manager setup complete");
			
			//create the client update manager and link it with the window events
			RunnableRepeatExecutor clientUpdater = new RunnableRepeatExecutor();
			clientUpdater.link(eventProcessor.getDisplay().getInputManager());
			
			//create and link player controllers so the user can play the game
			Arrays.asList(
					new PlayerController(client),
					new InteractionController(client, cameraGrip.getCamera(), 200, 300, 5),
					cameraGrip
			).forEach(controller -> {
				controller.link(eventProcessor.getDisplay().getInputManager());
				clientUpdater.getRunnables().add(controller);
			});
			
			System.out.println("game setup complete");

			//start the updater
			clientUpdater.start();
			
			//wait for the Display, and thus InputManager, to be created, then link the game manager to the display so that the escape button and x button exit the game
			eventProcessor.getDisplay().getInputManager().getWindowCloseHandlers().add(this);
			eventProcessor.getDisplay().getInputManager().getKeyHandlers().add(this);
			
			//display the window now that everything is set up.
			eventProcessor.setReadyToDisplay();
			
			System.out.println("display ready");
			
			//wait until the game should exit
			synchronized(this) {
				while(!exit) {
					try {
						this.wait();
					} catch(InterruptedException e) {
						e.printStackTrace(); //the game manager should never be interrupted
					}
				}
			}
			
			//TODO need to fix ordering of these things
			System.out.print("exiting... ");
			client.stop();
			clientUpdater.stop();
			clientUpdater.waitUntilFinished();
			renderManager.waitForExit();
			eventProcessor.waitForExit();
			audio.exit();
			System.out.println("done!");
		}
		
		/** can be called from any thread to exit the game **/
		public synchronized void exit() {
			exit = true;
			notifyAll(); //notify that ClientManager should exit
		}

		@Override
		public void windowClose() {
			exit();
		}

		@Override
		public void keyboardButton(int key, int scancode, int action, int mods) {
			if(key == Controls.KEYBIND_QUIT && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
				exit();
			} else if(key == Controls.KEYBIND_FULLSCREEN && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
	            eventProcessor.getDisplay().setFullscreen(!eventProcessor.getDisplay().getFullscreen());
	        }
		}

		@Override
		public void link(InputManager manager) {
			manager.getKeyHandlers().add(this);
		}

		@Override
		public void unlink(InputManager manager) {
			manager.getKeyHandlers().remove(this);
		}
	}
}