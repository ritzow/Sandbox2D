package ritzow.solomon.engine.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import ritzow.solomon.engine.audio.ClientAudioSystem;
import ritzow.solomon.engine.audio.Sounds;
import ritzow.solomon.engine.graphics.Camera;
import ritzow.solomon.engine.graphics.GraphicsUpdater;
import ritzow.solomon.engine.graphics.ModelRenderer;
import ritzow.solomon.engine.graphics.Models;
import ritzow.solomon.engine.input.Controls;
import ritzow.solomon.engine.input.EventProcessor;
import ritzow.solomon.engine.input.InputManager;
import ritzow.solomon.engine.input.controller.InteractionController;
import ritzow.solomon.engine.input.controller.PlayerController;
import ritzow.solomon.engine.input.controller.TrackingCameraController;
import ritzow.solomon.engine.input.handler.KeyHandler;
import ritzow.solomon.engine.input.handler.WindowCloseHandler;
import ritzow.solomon.engine.network.Client;
import ritzow.solomon.engine.util.ClientUpdater;
import ritzow.solomon.engine.world.base.ClientWorldUpdater;
import ritzow.solomon.engine.world.base.DefaultWorld;
import ritzow.solomon.engine.world.base.DefaultWorldRenderer;
import ritzow.solomon.engine.world.base.ModelRenderProgram;
import ritzow.solomon.engine.world.entity.PlayerEntity;

public final class StartClient {
	public static void main(String... args) throws IOException {
		//disables native pointer/function checks
		//org.lwjgl.system.Configuration.DISABLE_CHECKS.set(true);
		
		final Client client = new Client();
		new Thread(client, "Client Thread").start();
		client.waitForSetup();
		
		SocketAddress address = args.length == 2 ? new InetSocketAddress(args[0], Integer.parseInt(args[1])) : new InetSocketAddress(InetAddress.getLocalHost(), 50000);
		System.out.println("Connecting to " + address);
		
		//attempt to connect to the server for one second, if the client connects, run the game code and event processor
		if(client.connectTo(address, 1000)) {
			System.out.println("Client connected to " + address);
			EventProcessor eventProcessor = new EventProcessor();
			new Thread(new ClientManager(client, eventProcessor), "Client Manager").start();
			eventProcessor.run(); //run the event processor on this thread
		} else {
			System.out.println("Client failed to connect to " + address);
			client.exit();
			client.waitUntilFinished();
		}
	}
	
	private static final class ClientManager implements Runnable, WindowCloseHandler, KeyHandler {
		private final Client client;
		private final EventProcessor eventProcessor;
		private volatile boolean exit;
		
		public ClientManager(Client client, EventProcessor eventManager) {
			this.client = client;
			this.eventProcessor = eventManager;
		}
		
		@Override
		public void run() {
			
			//initializes OpenAL
			ClientAudioSystem audio = new ClientAudioSystem();
			
			try {
				Sounds.loadAll(new File("resources/assets/audio"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			audio.setVolume(3.0f);
			
			//wait for the client to receive the world and return it
			final DefaultWorld world = (DefaultWorld)client.getWorld(); //TODO bad design, what if the world isnt a DefaultWorld?
			world.setAudioSystem(audio);
			
			//wait for the Display, and thus InputManager, to be created, then link the game manager to the display so that the escape button and x button exit the game
			eventProcessor.waitForSetup();
			eventProcessor.getDisplay().getInputManager().getWindowCloseHandlers().add(this);
			eventProcessor.getDisplay().getInputManager().getKeyHandlers().add(this);
			
			Camera camera = new Camera(0, 0, 1);
			
			//start the graphics manager, which will load all models into OpenGL and setup the OpenGL context.
			GraphicsUpdater graphicsManager = new GraphicsUpdater(eventProcessor.getDisplay(), graphics -> {
				try {
					ModelRenderProgram program = new ModelRenderProgram(new FileInputStream("resources/shaders/vertexShader"), 
							new FileInputStream("resources/shaders/fragmentShader"), camera);
					graphics.getRenderers().add(new ModelRenderer(Models.forIndex(Models.CLOUDS_INDEX), program));
					graphics.getRenderers().add(new DefaultWorldRenderer(program, camera, world));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			
			new Thread(graphicsManager, "Graphics Manager").start();
			
			//wait for the graphics manager to finish setup.
			graphicsManager.waitForSetup();

			//create the client update manager and link it with the window events
			ClientUpdater clientUpdater = new ClientUpdater();
			clientUpdater.link(eventProcessor.getDisplay().getInputManager());
			
			//get the client's player object, which has already been added to the world
			PlayerEntity player = client.getPlayer();
			
			//create and link player controllers so the user can play the game
			Arrays.asList(
					new PlayerController(player, world, client),
					new InteractionController(player, world, camera, 200, true),
					new TrackingCameraController(camera, audio, player, 0.005f, 0.05f, 0.6f)
			).forEach(controller -> {
				controller.link(eventProcessor.getDisplay().getInputManager());
				clientUpdater.getUpdatables().add(controller);
			});
			
			//start the client updater
			new Thread(clientUpdater, "Client Updater Thread").start();
			
			//start updating the world
			ClientWorldUpdater worldUpdater = new ClientWorldUpdater(world);
			new Thread(worldUpdater, "World Updater Thread").start();
			
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
			client.exit();
			client.waitUntilFinished();
			worldUpdater.exit();
			clientUpdater.exit();
			graphicsManager.exit();
			graphicsManager.waitUntilFinished();
			eventProcessor.exit();
	 		eventProcessor.waitUntilFinished();
			worldUpdater.waitUntilFinished();
			audio.shutdown();

			//make sure the client updater and event manager are closed before exiting the "game" thread
	 		clientUpdater.waitUntilFinished();
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