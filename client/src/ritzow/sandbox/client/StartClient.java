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
import org.lwjgl.glfw.GLFWErrorCallback;
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
import ritzow.sandbox.client.input.controller.CameraController;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.PlayerController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.network.UpdateClient;
import ritzow.sandbox.client.ui.Cursors;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;

public class StartClient {
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
	
	private static final boolean USE_OPENGL_4_6 = false;

	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("Game Thread");
		InetSocketAddress serverSocket =
				Utility.getAddressOrDefault(args, 0, InetAddress.getLocalHost(), Protocol.DEFAULT_SERVER_PORT_UDP);
		var localSocket = new InetSocketAddress(Utility.getPublicAddress(Inet4Address.class), 0);
		System.out.print("Connecting to " + Utility.formatAddress(serverSocket)
			+ " from " + Utility.formatAddress(localSocket) + "... ");
		client = UpdateClient.startConnection(localSocket, serverSocket);
		GameLoop.addTask(client::update); //add client updates to the game loop
		GameLoop.addTask(StartClient::setupPart1);
		GameLoop.run();
	}

	private static void setupPart1() throws IOException {
		if(client.getStatus() == UpdateClient.Status.CONNECTED) {
			System.out.println("connected.");
			audio = setupAudio();
			display = setupGLFW();
			input = display.getEventDelegator();

			//mute audio when window is in background
			input.windowFocusHandlers().add(focused -> audio.setVolume(focused ? 1.0f : 0.0f));
			input.windowCloseHandlers().add(() -> GameLoop.spawnTask(StartClient::shutdown));
			input.keyboardHandlers().add((key, scancode, action, mods) -> {
				if(action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
					switch(key) {
					case ControlScheme.KEYBIND_QUIT:
						GameLoop.spawnTask(StartClient::shutdown);
						break;
					case ControlScheme.KEYBIND_FULLSCREEN:
						display.toggleFullscreen();
						break;
					}
				}
			});

			camera = new Camera(0, 0, 1);
			GameLoop.replaceTask(StartClient::setupPart2);
		} else if(client.getStatus() != UpdateClient.Status.CONNECTING) {
			GameLoop.endTask();
		}
	}
	
	private static long UPDATE_SKIP_THRESHOLD_NANOSECONDS = Utility.millisToNanos(100);
	private static long lastWorldUpdate;
	private static long lastCameraUpdateTime;

	private static void setupPart2() throws InterruptedException, IOException {
		if(client.getWorld() != null && client.getPlayer() != null) {
			player = client.getPlayer();
			cameraGrip = new TrackingCameraController(camera, audio, player, 2.5f, 0.05f, 0.6f);
			cameraGrip.link(input);

			playerControls = new PlayerController(client);
			playerControls.link(input);

			interactionControls = new InteractionController(client, Protocol.BLOCK_BREAK_RANGE);
			interactionControls.link(input);

			world = client.getWorld();
			renderer = setupRenderer(display, world, cameraGrip);

			//display the window now that everything is set up.
			display.show();

			lastWorldUpdate = System.nanoTime();
			lastCameraUpdateTime = System.nanoTime();
			GameLoop.replaceTask(StartClient::updateGame);
		}
	}

	private static void updateGame() throws TimeoutException, IOException {
		if(client.getStatus() == UpdateClient.Status.CONNECTED) {
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
		} else {
			shutdown();
		}
	}

	private static void shutdown() {
		System.out.print("Exiting... ");
		client.startDisconnect(); //TODO uhhh
		//TODO add a task that removes all tasks
		renderer.close(display);
		display.destroy();
		audio.close();
		glfwTerminate();
		System.out.println("done!");
	}

	private static RenderManager setupRenderer(Display display, World world, CameraController cameraGrip) throws IOException {
		var renderer = display.getRenderManager();
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

		renderer.getRenderers().add(new ClientWorldRenderer(program, cameraGrip.getCamera(), world));

		//var ui = new UserInterfaceRenderer(program);
		//renderer.getRenderers().add(ui);

		return renderer;
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
		display.setCursor(Cursors.load(Path.of("resources/assets/textures/cursors/pickaxe32.png"), 0, 0.66f));
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
