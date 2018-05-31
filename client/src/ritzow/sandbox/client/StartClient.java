package ritzow.sandbox.client;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import ritzow.sandbox.client.Client.ConnectionFailedException;
import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.audio.DefaultAudioSystem;
import ritzow.sandbox.client.audio.OpenALAudioSystem;
import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.client.audio.SoundInfo;
import ritzow.sandbox.client.audio.WAVEDecoder;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.client.input.EventDelegator;
import ritzow.sandbox.client.input.EventProcessor;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.PlayerController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.SharedConstants;
import ritzow.sandbox.util.TaskQueue;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;

public final class StartClient {
	public static void main(String... args) throws IOException {
		InetAddress serverAddress = args.length > 0 ? InetAddress.getByName(args[0]) : InetAddress.getLocalHost();
		InetSocketAddress serverSocket = new InetSocketAddress(serverAddress, Protocol.DEFAULT_SERVER_UDP_PORT);
		
		try {
			System.out.print("Connecting to " + serverAddress.getHostAddress() + " on port " + serverSocket.getPort() + "... ");
			Client client = Client.open(new InetSocketAddress(Utility.getPublicAddress(Inet4Address.class), 0), serverSocket);
			System.out.println("connected!");
			EventProcessor eventProcessor = new EventProcessor();
			new Thread(() -> run(eventProcessor, client), "Game Thread").start();
			Thread.currentThread().setName("GLFW Event Thread");
			eventProcessor.run(); //run the event processor on this thread
		} catch(ConnectionFailedException e) {
			System.out.println(e.getMessage());
		}
	}
	
	private static volatile boolean exit;
	
	private static void run(EventProcessor eventProcessor, Client client) {
		//wait for the client to receive the world and return it
		long startTime = System.nanoTime();
		
		AudioSystem audio = OpenALAudioSystem.getAudioSystem();
		DefaultAudioSystem.setDefault(audio);
		
		var soundFiles = Map.ofEntries(
			Map.entry("dig.wav", Sound.BLOCK_BREAK),
			Map.entry("place.wav", Sound.BLOCK_PLACE),
			Map.entry("pop.wav", Sound.POP),
			Map.entry("throw.wav", Sound.THROW),
			Map.entry("snap.wav", Sound.SNAP)
		);
		
		for(var entry : soundFiles.entrySet()) {
			try {
				SoundInfo info = WAVEDecoder.decode(
						Files.newInputStream(Path.of("resources/assets/audio", entry.getKey())));
				audio.registerSound(entry.getValue().code(), info);
			} catch(NoSuchFileException e) {
				System.out.println("The file " + e.getFile() + " does not exist");
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		
		audio.setVolume(1.0f);
		
		eventProcessor.waitForSetup();
		EventDelegator input = eventProcessor.getDisplay().getInputManager();
		
		//mute audio in background
		//input.windowFocusHandlers().add(focused -> audio.setVolume(focused ? 1.0f : 0.0f));
		
		var cameraGrip = new TrackingCameraController(new Camera(0, 0, 1), audio, client.getPlayer(), 0.005f, 0.05f, 0.6f);
		cameraGrip.link(input);
		
		var playerController = new PlayerController(client);
		playerController.link(input);
		
		var interactionController = new InteractionController(client, cameraGrip.getCamera(), 200, 300, 5);
		interactionController.link(input);
		
		input.keyboardHandlers().add((key, scancode, action, mods) -> {
			if(action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
				if(key == ControlScheme.KEYBIND_QUIT)
					exit = true;
				else if(key == ControlScheme.KEYBIND_FULLSCREEN)
		            eventProcessor.getDisplay().toggleFullscreen();
			}
		});

		input.windowCloseHandlers().add(() -> exit = true);
		client.setOnDisconnect(() -> exit = true);
		
		var world = client.getWorld();
		var renderer = eventProcessor.getDisplay().getRenderManager();
		renderer.initialize();
		renderer.getRenderers().add(createRenderer(world, cameraGrip.getCamera()));
		
		//display the window now that everything is set up.
		eventProcessor.setReadyToDisplay();
		System.out.println("Rendering started, setup took " + Utility.millisSince(startTime) + " ms");
		
		TaskQueue queue = new TaskQueue();
		client.setTaskQueue(queue);
		long previousTime = System.nanoTime();
		while(!exit) {
			queue.run(); //process received packets
			if(client.isConnected()) {
				previousTime = eventProcessor.getDisplay().focused() ? Utility.updateWorld(world, previousTime, 
						SharedConstants.MAX_TIMESTEP, SharedConstants.TIME_SCALE_NANOSECONDS) : System.nanoTime();
				cameraGrip.update();
				interactionController.update();
				renderer.run();
				Utility.sleep(1);
			}
		}
		
		System.out.print("Exiting... ");
		renderer.shutdown();
		eventProcessor.stop();
		if(client.isConnected())
			client.disconnect();
		audio.close();
		System.out.println("done!");
	}
	
	//to be run on game update thread (rendering thread)
	private static ClientWorldRenderer createRenderer(World world, Camera camera) {
		try {
			int indices = GraphicsUtility.uploadIndexData(0, 1, 2, 0, 2, 3);
			
			int positions = GraphicsUtility.uploadVertexData(
					-0.5f,	 0.5f,
					-0.5f,	-0.5f,
					0.5f,	-0.5f,
					0.5f,	 0.5f
			);
			
			TextureData dirt = Textures.loadTextureName("dirt"),
						grass = Textures.loadTextureName("grass"),
						face = Textures.loadTextureName("greenFace"),
						red = Textures.loadTextureName("redSquare");
			TextureAtlas atlas = Textures.buildAtlas(grass, dirt, face, red);
			
			ModelRenderProgram modelProgram = new ModelRenderProgram(
					new Shader(Files.newInputStream(Path.of("resources/shaders/modelVertexShader")), ShaderType.VERTEX),
					new Shader(Files.newInputStream(Path.of("resources/shaders/modelFragmentShader")), ShaderType.FRAGMENT), 
					atlas.texture()
			);
			
			register(modelProgram, RenderConstants.MODEL_DIRT_BLOCK, indices, positions, atlas, dirt);
			register(modelProgram, RenderConstants.MODEL_GRASS_BLOCK, indices, positions, atlas, grass);
			register(modelProgram, RenderConstants.MODEL_GREEN_FACE, indices, positions, atlas, face);
			register(modelProgram, RenderConstants.MODEL_RED_SQUARE, indices, positions, atlas, red);
			GraphicsUtility.checkErrors();
			return new ClientWorldRenderer(modelProgram, camera, world);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void register(ModelRenderProgram prog, int id, int indices, int positions, TextureAtlas atlas, TextureData data) {
		prog.register(id, new RenderData(6, indices, positions, GraphicsUtility.uploadVertexData(atlas.getCoordinates(data))));
	}
}