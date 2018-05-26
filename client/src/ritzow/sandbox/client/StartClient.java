package ritzow.sandbox.client;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ritzow.sandbox.client.Client.ConnectionFailedException;
import ritzow.sandbox.client.audio.ClientAudioSystem;
import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.client.audio.WAVEDecoder;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.client.input.EventProcessor;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.controller.CameraController;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.PlayerController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.input.handler.WindowFocusHandler;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.RepeatUpdater;
import ritzow.sandbox.util.SharedConstants;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;

public final class StartClient {
	public static void main(String... args) throws IOException {
		InetAddress serverAddress = args.length > 0 ? InetAddress.getByName(args[0]) : InetAddress.getLocalHost();
		InetSocketAddress serverSocket = new InetSocketAddress(serverAddress, Protocol.DEFAULT_SERVER_UDP_PORT);
		
		try {
			System.out.print("Connecting to " + serverAddress.getHostAddress() + " on port " + serverSocket.getPort() + "... ");
			//wildcard address, and any port
			Client client = Client.open(new InetSocketAddress(getLocalAddress(Inet4Address.class), 0), serverSocket);
			System.out.println("connected!");
			EventProcessor eventProcessor = new EventProcessor();
			new Thread(() -> run(eventProcessor, client), "Client Manager").start();
			eventProcessor.run(); //run the event processor on this thread
		} catch(ConnectionFailedException e) {
			System.out.println("failed to connect: " + e.getMessage());
		}
	}
	
	private static boolean exit;
	private static Object exitLock = new Object();
	
	private static void exit() {
		exit = true;
		Utility.notify(exitLock);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends InetAddress> T getLocalAddress(Class<T> addressType) throws SocketException {
		Set<InetAddress> addresses = NetworkInterface.networkInterfaces()
			.filter(network -> {
				try {
					return network.isUp() && !network.isLoopback();
				} catch (SocketException e) {
					e.printStackTrace();
					return false;
				}
			})
			.map(network -> network.inetAddresses())
			.reduce((a, b) -> Stream.concat(a, b))
			.get().filter(address -> addressType.isInstance(address))
			.collect(Collectors.toSet());
		if(addresses.isEmpty())
			throw new RuntimeException("No address of type " + addressType + " available");
		return (T)addresses.iterator().next();
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
			
			TextureData dirt = Textures.loadTextureName("dirt");
			TextureData grass = Textures.loadTextureName("grass");
			TextureData face = Textures.loadTextureName("greenFace");
			TextureData red = Textures.loadTextureName("redSquare");
			TextureAtlas atlas = Textures.buildAtlas(grass, dirt, face, red);
			
			ModelRenderProgram modelProgram = new ModelRenderProgram(
					new Shader(Files.newInputStream(Paths.get("resources/shaders/modelVertexShader")), ShaderType.VERTEX),
					new Shader(Files.newInputStream(Paths.get("resources/shaders/modelFragmentShader")), ShaderType.FRAGMENT), 
					atlas.texture()
			);
			
			modelProgram.register(RenderConstants.MODEL_DIRT_BLOCK,	
					new RenderData(6, indices, positions, 
					GraphicsUtility.uploadVertexData(atlas.getTextureCoordinates(dirt))));
			modelProgram.register(RenderConstants.MODEL_GRASS_BLOCK,
					new RenderData(6, indices, positions, 
					GraphicsUtility.uploadVertexData(atlas.getTextureCoordinates(grass))));
			modelProgram.register(RenderConstants.MODEL_GREEN_FACE,	
					new RenderData(6, indices, positions, 
					GraphicsUtility.uploadVertexData(atlas.getTextureCoordinates(face))));
			modelProgram.register(RenderConstants.MODEL_RED_SQUARE,	
					new RenderData(6, indices, positions, 
					GraphicsUtility.uploadVertexData(atlas.getTextureCoordinates(red))));
			GraphicsUtility.checkErrors();
			renderManager.getRenderers().add(new ClientWorldRenderer(modelProgram, cameraGrip.getCamera(), world));
		} catch (IOException | OpenGLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void run(EventProcessor eventProcessor, Client client) {
		eventProcessor.waitForSetup();
		InputManager input = eventProcessor.getDisplay().getInputManager();
		
		//wait for the client to receive the world and return it
		World world = client.getWorld();
		System.out.println("Received world from server.");
		
		var audio = ClientAudioSystem.getAudioSystem();
		
		var soundFiles = Map.ofEntries(
			Map.entry("dig.wav", Sound.BLOCK_BREAK),
			Map.entry("place.wav", Sound.BLOCK_PLACE),
			Map.entry("pop.wav", Sound.POP),
			Map.entry("throw.wav", Sound.THROW),
			Map.entry("snap.wav", Sound.SNAP)
		);
		
		try {
			for(var entry : soundFiles.entrySet()) {
				audio.registerSound(entry.getValue().code(), 
						WAVEDecoder.decode(
								Files.newInputStream(Paths.get("resources/assets/audio", entry.getKey()))));
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		audio.setVolume(1.0f);
		
		//mute audio in background
		input.getWindowFocusHandlers().add(focused -> audio.setVolume(focused ? 1.0f : 0.0f));
		
		var player = client.getPlayer();
		System.out.println("Received player from server.");
		
		var cameraGrip = new TrackingCameraController(new Camera(0, 0, 1), audio, player, 0.005f, 0.05f, 0.6f);
		cameraGrip.link(input);
		
		var playerController = new PlayerController(client);
		playerController.link(input);
		
		var interactionController = new InteractionController(client, cameraGrip.getCamera(), 200, 300, 5);
		interactionController.link(input);
		
		WorldUpdater updater = new WorldUpdater(world);
		updater.link(input);
		
		RenderManager renderManager = eventProcessor.getDisplay().getRenderManager();
		RepeatUpdater gameUpdater = new RepeatUpdater(() -> initGraphics(renderManager, world, cameraGrip), renderManager::shutdown);
		
		gameUpdater.repeatTasks().add(client.onReceiveMessageTask());
		gameUpdater.repeatTasks().add(playerController);
		gameUpdater.repeatTasks().add(interactionController);
		gameUpdater.repeatTasks().add(cameraGrip);
		gameUpdater.repeatTasks().add(updater);
		gameUpdater.repeatTasks().add(renderManager);
		gameUpdater.repeatTasks().add(() -> Utility.sleep(1));
		
		gameUpdater.start("Game updater");
		gameUpdater.waitForSetup();
		
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
		
//		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//		}));
		
		//display the window now that everything is set up.
		eventProcessor.setReadyToDisplay();
		System.out.println("Rendering started.");
		
		//wait until the window is closed or escape key is pressed
		Utility.waitOnCondition(exitLock, () -> exit);
		
		System.out.print("Exiting... ");
		gameUpdater.stop();
		eventProcessor.stop();
		if(client.isConnected()) client.disconnect();
		audio.close();
		ClientAudioSystem.shutdown();
		System.out.println("done!");
	}
	
	private static class WorldUpdater implements Runnable, WindowFocusHandler {
		private volatile long previousTime = System.nanoTime();
		private volatile boolean focused = true;
		private final World world;
		
		public WorldUpdater(World world) {
			this.world = world;
		}
		
		public void run() {
			if(focused) {
				previousTime = Utility.updateWorld(world, previousTime, 
						SharedConstants.MAX_TIMESTEP, 
						SharedConstants.TIME_SCALE_NANOSECONDS);
			}
		}
		
		@Override
		public void windowFocus(boolean focused) {
			if(focused)
				previousTime = System.nanoTime();
			this.focused = focused;
		}

		public void link(InputManager m) {
			m.getWindowFocusHandlers().add(this);
		}
	}
}