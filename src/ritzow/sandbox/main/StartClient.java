package ritzow.sandbox.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;

import ritzow.sandbox.client.Client;
import ritzow.sandbox.client.ClientWorld;
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
import ritzow.sandbox.util.ClientUpdater;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class StartClient {
	public static void main(String... args) throws IOException {
		final Client client = new Client();
		client.start();
		client.waitUntilStarted();
		
		SocketAddress address = args.length == 2 ? new InetSocketAddress(args[0], Integer.parseInt(args[1])) : new InetSocketAddress(InetAddress.getLocalHost(), 50000);
		System.out.println("Connecting to " + address);
		
		//attempt to connect to the server for one second, if the client connects, run the game code and event processor
		if(client.connectTo(address, 1000)) {
			System.out.println("Client connected to " + address);
			EventProcessor eventProcessor = new EventProcessor();
			new Thread(new ClientStarter(client, eventProcessor), "Client Manager").start();
			
			eventProcessor.run(); //run the event processor on this thread
		} else {
			System.out.println("Client failed to connect to " + address);
			client.stop();
			client.waitUntilStopped();
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
			//initializes OpenAL
			ClientAudioSystem audio = new ClientAudioSystem();
			//TODO make sound register system, something like AudioSystem::registerSound method to associate sounds with a system, rather than being global
			try {
				Sounds.loadAll(new File("resources/assets/audio"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			//set the audio manager to a reasonable gain
			audio.setVolume(1.0f);
			
			//wait for the client to receive the world and return it
			World world = client.getWorld();
			world.setAudioSystem(audio);
			
			//wait for the Display, and thus InputManager, to be created, then link the game manager to the display so that the escape button and x button exit the game
			eventProcessor.waitForSetup();
			eventProcessor.getDisplay().getInputManager().getWindowCloseHandlers().add(this);
			eventProcessor.getDisplay().getInputManager().getKeyHandlers().add(this);
			
			Camera camera = new Camera(0, 0, 1);
			
			//get the client's player object, which has already been added to the world
			PlayerEntity player = client.getPlayer();
			CameraController cameraGrip = new TrackingCameraController(camera, audio, player, 0.005f, 0.05f, 0.6f);
			cameraGrip.link(eventProcessor.getDisplay().getInputManager());
			
			//start the graphics manager, which will load all models into OpenGL and setup the OpenGL context.
			RenderManager renderManager = eventProcessor.getDisplay().getRenderManager();
			
			renderManager.addRenderTask(graphics -> {
				try {
					//load shader programs on renderer startup so that it is done in OpenGL thread, otherwise the program will crash
					ModelRenderProgram modelProgram = new ModelRenderProgram(
							new Shader(new FileInputStream("resources/shaders/modelVertexShader"), org.lwjgl.opengl.GL20.GL_VERTEX_SHADER),
							new Shader(new FileInputStream("resources/shaders/modelFragmentShader"), org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER), 
							camera
					);
					
					LightRenderProgram lightProgram = new LightRenderProgram(
							new Shader(new FileInputStream("resources/shaders/lightVertexShader"), org.lwjgl.opengl.GL20.GL_VERTEX_SHADER),
							new Shader(new FileInputStream("resources/shaders/lightFragmentShader"), org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER),
							camera
					);
					
					graphics.getRenderers().add(new ClientWorldRenderer(cameraGrip, modelProgram, lightProgram, (ClientWorld)world));
				} catch (IOException | OpenGLException e) {
					throw new RuntimeException(e);
				}
			});
			
			renderManager.start();
			renderManager.waitForSetup();
			
			//create the client update manager and link it with the window events
			ClientUpdater clientUpdater = new ClientUpdater();
			clientUpdater.link(eventProcessor.getDisplay().getInputManager());
			
			//create and link player controllers so the user can play the game
			Arrays.asList(
					new PlayerController(player, world, client),
					new InteractionController(player, world, camera, 200, 300, 5)
			).forEach(controller -> {
				controller.link(eventProcessor.getDisplay().getInputManager());
				clientUpdater.getUpdatables().add(controller);
			});
			
			//start the updater
			clientUpdater.start();
			
			//display the window now that everything is set up.
			eventProcessor.setReadyToDisplay();
			
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
			
			//start exiting threads
			System.out.print("Exiting client... ");
			renderManager.waitForExit();
			eventProcessor.waitForExit();
			client.stop();
			clientUpdater.stop();
			client.waitUntilStopped();
			clientUpdater.waitUntilFinished();
			audio.shutdown();
			System.out.println("client exited");
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