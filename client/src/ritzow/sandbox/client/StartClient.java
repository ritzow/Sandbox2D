package ritzow.sandbox.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import ritzow.sandbox.client.audio.ClientAudioSystem;
import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.client.audio.WAVEDecoder;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.client.input.EventProcessor;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.controller.CameraController;
import ritzow.sandbox.client.input.controller.Controller;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.PlayerController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.input.handler.InputHandler;
import ritzow.sandbox.client.input.handler.WindowFocusHandler;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.RepeatUpdater;
import ritzow.sandbox.util.SharedConstants;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;

public final class StartClient {
	public static void main(String... args) throws SocketException, UnknownHostException {
		InetAddress serverAddress = args.length > 0 ? InetAddress.getByName(args[0]) : InetAddress.getLocalHost();
		InetSocketAddress serverSocket = new InetSocketAddress(serverAddress, Protocol.DEFAULT_SERVER_UDP_PORT);
		
		Client client = new Client(new InetSocketAddress(0), serverSocket); //wildcard address, and any port
		System.out.print("Connecting to " + serverAddress.getHostAddress() + " on port " + serverSocket.getPort() + "... ");
		
		if(client.connect()) {
			System.out.println("connected!");
			EventProcessor eventProcessor = new EventProcessor();
			new Thread(() -> run(eventProcessor, client), "Client Manager").start();
			eventProcessor.run(); //run the event processor on this thread
		} else {
			System.out.println("failed to connect.");
		}
	}
	
	private static boolean exit;
	private static Object exitLock = new Object();
	
	private static void exit() {
		synchronized(exitLock) {
			exit = true;
			exitLock.notifyAll();
		}
	}
	
	//to be run on game update thread (rendering thread)
	private static void initGraphics(RenderManager renderManager, World world, CameraController cameraGrip) {
		try {
			renderManager.initialize(); //set up opengl
			
			int indices = GraphicsUtility.uploadIndexData(0, 1, 2, 0, 2, 3);
			
			int positions = GraphicsUtility.uploadVertexData(
					-0.5f,	 0.5f,
					-0.5f,	-0.5f,
					0.5f,	-0.5f,
					0.5f,	 0.5f
			);
			
			int textureCoordinates = GraphicsUtility.uploadVertexData(
					0f, 1f,
					0f, 0f,
					1f, 0f,
					1f, 1f	
			);
			
			try {
				Textures.loadAll(new File("resources/assets/textures"));
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
			
			ModelRenderProgram modelProgram = new ModelRenderProgram(
					new Shader(new FileInputStream("resources/shaders/modelVertexShader"), ShaderType.VERTEX),
					new Shader(new FileInputStream("resources/shaders/modelFragmentShader"), ShaderType.FRAGMENT), 
					cameraGrip.getCamera()
			);
			
			modelProgram.register(RenderConstants.MODEL_GRASS_BLOCK,new RenderData(6, indices, positions, textureCoordinates, Textures.GRASS.id));
			modelProgram.register(RenderConstants.MODEL_DIRT_BLOCK,	new RenderData(6, indices, positions, textureCoordinates, Textures.DIRT.id));
			modelProgram.register(RenderConstants.MODEL_GREEN_FACE,	new RenderData(6, indices, positions, textureCoordinates, Textures.GREEN_FACE.id));
			modelProgram.register(RenderConstants.MODEL_RED_SQUARE,	new RenderData(6, indices, positions, textureCoordinates, Textures.RED_SQUARE.id));
			//modelProgram.register(23, new RenderData(6, indices, positions, textureCoordinates, Textures.ATLAS.id));
			
			LightRenderProgram lightProgram = new LightRenderProgram(
					new Shader(new FileInputStream("resources/shaders/lightVertexShader"), ShaderType.VERTEX),
					new Shader(new FileInputStream("resources/shaders/lightFragmentShader"), ShaderType.FRAGMENT),
					cameraGrip.getCamera()
			);
			renderManager.getRenderers().add(new ClientWorldRenderer(modelProgram, lightProgram, world));
			GraphicsUtility.checkErrors();
		} catch (IOException | OpenGLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void run(EventProcessor eventProcessor, Client client) {
		eventProcessor.waitForSetup();
		
		//wait for the client to receive the world and return it
		World world = client.getWorld();
		System.out.println("Received world from server.");
		
		ClientAudioSystem audio = ClientAudioSystem.getAudioSystem();
		
		Map<String, Sound> soundFiles = Map.ofEntries(
			Map.entry("dig.wav", Sound.BLOCK_BREAK),
			Map.entry("place.wav", Sound.BLOCK_PLACE),
			Map.entry("pop.wav", Sound.POP),
			Map.entry("throw.wav", Sound.THROW),
			Map.entry("snap.wav", Sound.SNAP)
		);
		
		try {
			for(Entry<String, Sound> entry : soundFiles.entrySet()) {
				audio.registerSound(entry.getValue().code(), WAVEDecoder.decode(
						new FileInputStream(new File("resources/assets/audio", entry.getKey()))));
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		audio.setVolume(1.0f);
		
		//mute audio in background
		eventProcessor.getDisplay().getInputManager().getWindowFocusHandlers()
			.add(focused -> audio.setVolume(focused ? 1.0f : 0.0f));
		
		ClientPlayerEntity player = client.getPlayer();
		System.out.println("Received player from server.");
		
		CameraController cameraGrip =
				new TrackingCameraController(new Camera(0, 0, 1), audio, player, 0.005f, 0.05f, 0.6f);
		
		//create and link player controllers so the user canControllergame
		Collection<Controller> controllers = List.of(
				new PlayerController(client),
				new InteractionController(client, cameraGrip.getCamera(), 200, 300, 5),
				cameraGrip
		);
		
		controllers.forEach(eventProcessor.getDisplay().getInputManager()::add);
		
		RenderManager renderManager = eventProcessor.getDisplay().getRenderManager();
		RepeatUpdater gameUpdater = new RepeatUpdater(() -> initGraphics(renderManager, world, cameraGrip), renderManager::shutdown);
		
		class WorldUpdater implements Runnable, WindowFocusHandler, InputHandler {
			private volatile long previousTime = System.nanoTime();
			private volatile boolean focused = true;
			
			public void run() {
				if(focused)
					previousTime = Utility.updateWorld(world, previousTime, SharedConstants.MAX_TIMESTEP, SharedConstants.TIME_SCALE_NANOSECONDS);
			}
			
			@Override
			public void windowFocus(boolean focused) {
				if(focused)
					previousTime = System.nanoTime();
				this.focused = focused;
			}

			@Override
			public void link(InputManager m) {
				m.getWindowFocusHandlers().add(this);
			}

			@Override
			public void unlink(InputManager m) {
				m.getWindowFocusHandlers().remove(this);
			}
		}
		
		WorldUpdater updater = new WorldUpdater();
		eventProcessor.getDisplay().getInputManager().add(updater);
		
		gameUpdater.getRepeatTasks().add(client.onReceiveMessageTask());
		gameUpdater.getRepeatTasks().addAll(controllers);
		gameUpdater.getRepeatTasks().add(updater);
		gameUpdater.getRepeatTasks().add(renderManager);
		gameUpdater.getRepeatTasks().add(() -> Utility.sleep(1));
		gameUpdater.start("Game updater");
		gameUpdater.waitForSetup();
		
		InputManager input = eventProcessor.getDisplay().getInputManager();
		input.getKeyHandlers().add((key, scancode, action, mods) -> {
			if(action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
				if(key == ControlScheme.KEYBIND_QUIT)
					exit();
				else if(key == ControlScheme.KEYBIND_FULLSCREEN)
		            eventProcessor.getDisplay().toggleFullscreen();
			}
		});

		input.getWindowCloseHandlers().add(StartClient::exit);
		client.setOnDisconnect(StartClient::exit);
		
		//display the window now that everything is set up.
		eventProcessor.setReadyToDisplay();
		System.out.println("Rendering started.");
		
		//wait until the window is closed or escape key is pressed
		Utility.waitOnCondition(exitLock, () -> exit);
		
		System.out.print("Exiting... ");
		eventProcessor.stop();
		gameUpdater.stop();
		if(client.isConnected()) client.disconnect();
		audio.close();
		ClientAudioSystem.shutdown();
		System.out.println("done!");
	}
}