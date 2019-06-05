package ritzow.sandbox.client;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.lwjgl.glfw.GLFWErrorCallback;
import ritzow.sandbox.client.GameLoop.GameTask;
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
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.PlayerController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.network.UpdateClient;
import ritzow.sandbox.client.network.UpdateClient.ClientEvent;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;

public class StartClient {
	private static InetSocketAddress serverAddress;
	private static InetSocketAddress localAddress;
	private static UpdateClient client;
	private static Display display;
	private static AudioSystem audio;
	private static EventDelegator input;
	private static ClientPlayerEntity player;
	private static World world;
	private static Camera camera;
	private static TrackingCameraController cameraGrip;
	private static PlayerController playerControls;
	private static InteractionController interactionControls;
	private static RenderManager renderer;
	private static ModelRenderProgram worldProgram;
	private static volatile boolean connect;
	private static long startupStart;
	
	private static long UPDATE_SKIP_THRESHOLD_NANOSECONDS = Utility.millisToNanos(100);
	private static long lastWorldUpdate;
	private static long lastCameraUpdateTime;
	
	private static final Queue<GameTask> concurrentEvents = new ConcurrentLinkedQueue<>();
	
	private static final boolean USE_OPENGL_4_6 = false;

	public static void main(String[] args) throws Exception {
		startupStart = System.nanoTime();
		serverAddress =	Utility.getAddressOrDefault(args, 0, InetAddress.getLocalHost(), Protocol.DEFAULT_SERVER_PORT_UDP);
		localAddress = new InetSocketAddress(Utility.getPublicAddress(Inet4Address.class), 0);
		Thread.currentThread().setName("Game Thread");
		setupInitial();
		GameLoop.run(StartClient::menuLoop, StartClient::runConcurrentEvents);
		System.out.println("done!");
	}
	
	private static void runConcurrentEvents() throws Exception {
		while(!concurrentEvents.isEmpty()) {
			concurrentEvents.poll().run();
		}
	}
	
	private static void menuLoop() {
		glfwPollEvents(); //process input/windowing events
		Utility.sleep(1); //reduce CPU usage
	}
	
	private static void connect() throws NumberFormatException, IOException {
		System.out.print("Connecting to " + Utility.formatAddress(serverAddress)
		+ " from " + Utility.formatAddress(localAddress) + "... ");
		client = UpdateClient.startConnection(localAddress, serverAddress);
		//client.setEventListener(ClientEvent.CONNECT_ACCEPTED, StartClient::onAccepted);
		client.setEventListener(ClientEvent.CONNECT__REJECTED, StartClient::onRejected);
		client.setEventListener(ClientEvent.DISCONNECTED, StartClient::onDisconnected);
		client.setEventListener(ClientEvent.WORLD_JOIN, StartClient::setupGameWorld);
		GameLoop.set(client::update, StartClient::runConcurrentEvents);
	}
	
	private static void onRejected() {
		GameLoop.set();
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void onDisconnected() {
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		GameLoop.set(); //stop the game loop now that the client is closed
		System.out.print("Exiting... ");
		renderer.close(display);
		display.destroy();
		audio.close();
		glfwTerminate();
	}
	
	private static void onConcurrentExit() {
		concurrentEvents.add(() -> {
			if(client == null) {
				System.out.print("Exiting... ");
				GameLoop.set();
				display.destroy();
				glfwTerminate();
				audio.close();
			} else {
				GameLoop.set(client::update);
				client.startDisconnect();
			}
		});
	}
	
	private static void setupInitial() throws IOException {
		audio = setupAudio();
		display = setupGLFW();
		input = display.getEventDelegator();
		setupRenderer(display);

		//mute audio when window is in background
		input.windowFocusHandlers().add(focused -> audio.setVolume(focused ? 1.0f : 0.0f));
		input.windowCloseHandlers().add(StartClient::onConcurrentExit); //TODO make this work pre-connect too
		input.keyboardHandlers().add((key, scancode, action, mods) -> {
			if(action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
				switch(key) {
					case ControlScheme.KEYBIND_CONNECT -> {
						if(!connect) {
							concurrentEvents.add(StartClient::connect);
							connect = true;
						}
					}
					case ControlScheme.KEYBIND_QUIT -> StartClient.onConcurrentExit();
					case ControlScheme.KEYBIND_FULLSCREEN -> display.toggleFullscreen();
				}
			}
		});
		
		//display the window now that everything is set up.
		display.show();
		System.out.println("Startup took " + Utility.formatTime(Utility.nanosSince(startupStart)));
	}

	private static void setupGameWorld(World world, ClientPlayerEntity player) {
		StartClient.world = world;
		StartClient.player = player;
		
		camera = new Camera(0, 0, 1);
		cameraGrip = new TrackingCameraController(camera, audio, player, 2.5f, 0.05f, 0.6f);
		cameraGrip.link(input);

		playerControls = new PlayerController(client);
		playerControls.link(input);

		interactionControls = new InteractionController(client, Protocol.BLOCK_BREAK_RANGE);
		interactionControls.link(input);

		renderer.getRenderers().add(new ClientWorldRenderer(worldProgram, cameraGrip.getCamera(), world));
		lastWorldUpdate = System.nanoTime();
		lastCameraUpdateTime = System.nanoTime();
		display.enableCursor(true);
		GameLoop.set(StartClient::updateGame, StartClient::runConcurrentEvents);
	}

	private static void updateGame() throws TimeoutException, IOException {
		glfwPollEvents(); //process input/windowing events
		client.update();
		lastWorldUpdate = updateWorld(display, world, lastWorldUpdate); //simulate world on client
		long camUpdateStart = System.nanoTime();
		cameraGrip.update(camUpdateStart - lastCameraUpdateTime); //update camera position and zoom
		lastCameraUpdateTime = camUpdateStart;
		interactionControls.update(camera, client, world, player, display.width(), display.height()); //block breaking/"bomb throwing"
		if(!display.minimized())
			renderer.run(display);
		Utility.sleep(1); //reduce CPU usage
	}
	
	private static void setupRenderer(Display display) throws IOException {
		renderer = display.getRenderManager();
		renderer.initialize(display);

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

		long start = System.nanoTime();
		var program = USE_OPENGL_4_6 ? createProgramFromSPIRV(atlas) : createProgramFromSource(atlas);
		System.out.println("Created model renderer in " + Utility.formatTime(System.nanoTime() - start) + ".");

		program.register(RenderConstants.MODEL_DIRT_BLOCK, getRenderData(indices, positions, atlas, dirt));
		program.register(RenderConstants.MODEL_GRASS_BLOCK, getRenderData(indices, positions, atlas, grass));
		program.register(RenderConstants.MODEL_GREEN_FACE, getRenderData(indices, positions, atlas, face));
		program.register(RenderConstants.MODEL_RED_SQUARE, getRenderData(indices, positions, atlas, red));
		GraphicsUtility.checkErrors();
		
		worldProgram = program;
		
		//var ui = new UserInterfaceRenderer(program);
		//renderer.getRenderers().add(ui);
	}

	private static ByteBuffer readIntoBuffer(Path file) throws IOException {
		ByteBuffer out = ByteBuffer.allocateDirect((int)Files.size(file));
		try(var channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
			channel.read(out);
		}
		return out.flip();
	}

	private static ModelRenderProgram createProgramFromSPIRV(TextureAtlas atlas) throws IOException {
		return new ModelRenderProgram(
				Shader.fromSPIRV(readIntoBuffer(Path.of("resources/shaders/modelVertexShader.spv")), ShaderType.VERTEX),
				Shader.fromSPIRV(readIntoBuffer(Path.of("resources/shaders/modelFragmentShader.spv")), ShaderType.FRAGMENT),
				atlas.texture()
		);
	}

	private static ModelRenderProgram createProgramFromSource(TextureAtlas atlas) throws IOException {
		return new ModelRenderProgram(
				Shader.fromSource(Files.readString(Path.of("resources/shaders/modelVertexShader")), ShaderType.VERTEX),
				Shader.fromSource(Files.readString(Path.of("resources/shaders/modelFragmentShader")), ShaderType.FRAGMENT),
				atlas.texture()
		);
	}
	
	private static long updateWorld(Display display, World world, long lastWorldUpdate) {
		long time = System.nanoTime();

		if(display.focused() && time - lastWorldUpdate < UPDATE_SKIP_THRESHOLD_NANOSECONDS) {
			return Utility.updateWorld(world, lastWorldUpdate, Protocol.MAX_UPDATE_TIMESTEP, Protocol.TIME_SCALE_NANOSECONDS);
		} else {
			return time;
		}
	}

	private static Display setupGLFW() throws IOException {
		if(!glfwInit())
			throw new RuntimeException("GLFW failed to initialize");
		glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err));
		Display display = new Display("Sandbox2D", 4, 5);
		var cursor = ClientUtility.loadGlfwImage(Path.of("resources/assets/textures/cursors/pickaxe32.png"));
		display.setCursor(ClientUtility.loadGlfwCursor(cursor, 0, 0.66f));
		display.enableCursor(false);
		display.setIcons(ClientUtility.loadGlfwImage(Path.of("resources/assets/textures/redSquare.png")));
		return display;
	}

	private static AudioSystem setupAudio() throws IOException {
		AudioSystem audio = OpenALAudioSystem.getAudioSystem();
		audio.setVolume(1.0f);
		DefaultAudioSystem.setDefault(audio);

		var sounds = Map.ofEntries(
			Map.entry("dig.wav", Sound.BLOCK_BREAK),
			Map.entry("place.wav", Sound.BLOCK_PLACE),
			Map.entry("pop.wav", Sound.POP),
			Map.entry("throw.wav", Sound.THROW),
			Map.entry("snap.wav", Sound.SNAP)
		);

		Path directory = Path.of("resources/assets/audio");

		for(var entry : sounds.entrySet()) {
			try(var in = Files.newInputStream(directory.resolve(entry.getKey()))) {
				SoundInfo info = WAVEDecoder.decode(in);
				audio.registerSound(entry.getValue().code(), info);
			}
		}
		return audio;
	}

	private static RenderData getRenderData(int indices, int positions, TextureAtlas atlas, TextureData data) {
		return new RenderData(6, indices, positions, GraphicsUtility.uploadVertexData(atlas.getCoordinates(data)));
	}
}
