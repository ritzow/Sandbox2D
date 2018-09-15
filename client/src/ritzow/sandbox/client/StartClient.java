package ritzow.sandbox.client;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ritzow.sandbox.client.audio.*;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.client.input.EventDelegator;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.PlayerController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.network.Client.ConnectionFailedException;
import ritzow.sandbox.client.ui.Cursors;
import ritzow.sandbox.client.ui.UserInterfaceRenderer;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.TaskQueue;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;

public class StartClient {
	private static boolean manualExit;
	private static long UPDATE_SKIP_THRESHOLD_NANOSECONDS = Utility.millisToNanos(100);

	public static void main(String[] args) throws IOException, InterruptedException {
		Thread.currentThread().setName("Game Thread");

		try {
			InetSocketAddress serverSocket = Utility.getAddressOrDefault(args, 0, InetAddress.getLocalHost(), Protocol.DEFAULT_SERVER_UDP_PORT);
			InetSocketAddress localSocket = new InetSocketAddress(Utility.getPublicAddress(Inet4Address.class), 0);

			ExecutorService runner = Executors.newSingleThreadExecutor();
			System.out.print("Connecting to " + Utility.formatAddress(serverSocket) + " from " + Utility.formatAddress(localSocket) + "... ");
			Client client = Client.connect(localSocket, serverSocket, runner);
			System.out.println("connected!");

			AudioSystem audio = setupAudio();
			Display display = setupGLFW();
			EventDelegator input = display.getEventDelegator();

			//mute audio when window is in background
			input.windowFocusHandlers().add(focused -> audio.setVolume(focused ? 1.0f : 0.0f));
			input.windowCloseHandlers().add(() -> manualExit = true);
			input.keyboardHandlers().add((key, scancode, action, mods) -> {
				if(action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
					switch(key) {
					case ControlScheme.KEYBIND_QUIT:
						manualExit = true;
						break;
					case ControlScheme.KEYBIND_FULLSCREEN:
						display.toggleFullscreen();
						break;
					}
				}
			});

			Camera camera = new Camera(0, 0, 1);

			var player = client.getPlayer();
			var cameraGrip = new TrackingCameraController(camera, audio, player, 2.5f, 0.05f, 0.6f);
			cameraGrip.link(input);

			var playerController = new PlayerController(client);
			playerController.link(input);

			var interactionController = new InteractionController(client, Protocol.BLOCK_BREAK_RANGE);
			interactionController.link(input);

			var world = client.getWorld();
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
			var program = createProgramFromSPIRV(atlas);
			System.out.println("took " + Utility.formatTime(System.nanoTime() - start) + " to create model renderer");

			program.register(RenderConstants.MODEL_DIRT_BLOCK, getRenderData(indices, positions, atlas, dirt));
			program.register(RenderConstants.MODEL_GRASS_BLOCK, getRenderData(indices, positions, atlas, grass));
			program.register(RenderConstants.MODEL_GREEN_FACE, getRenderData(indices, positions, atlas, face));
			program.register(RenderConstants.MODEL_RED_SQUARE, getRenderData(indices, positions, atlas, red));
			GraphicsUtility.checkErrors();

			renderer.getRenderers().add(new ClientWorldRenderer(program, cameraGrip.getCamera(), world));

			var ui = new UserInterfaceRenderer(program);
			//renderer.getRenderers().add(ui);

			//set up synchronous packet processing structures
			TaskQueue packetQueue = new TaskQueue();
			client.setExecutor(packetQueue::add);
			runner.shutdown(); //transfer remaining tasks
			runner.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

			//display the window now that everything is set up.
			display.show();

			long lastWorldUpdate = System.nanoTime();
			long lastCameraUpdateTime = System.nanoTime();
			try {
				while(!manualExit && client.isConnected()) {
					glfwPollEvents(); //process input/windowing events
					packetQueue.run(); //processing packets received from server
					lastWorldUpdate = updateWorld(display, world, lastWorldUpdate); //simulate world on client
					long camUpdateStart = System.nanoTime();
					cameraGrip.update(camUpdateStart - lastCameraUpdateTime); //update camera position and zoom
					lastCameraUpdateTime = camUpdateStart;
					interactionController.update(camera, client, world, player, display.width(), display.height()); //block breaking/"bomb throwing"
					if(display.focused()) {
						ui.update();
						renderer.run(display);
					}
					Utility.sleep(1); //reduce CPU usage
				}
			} finally {
				System.out.print("Exiting... ");
				renderer.close(display);
				display.destroy();
				if(client.isConnected()) {
					client.disconnect();
				}
				audio.close();
				glfwTerminate();
				System.out.println("done!");
			}
		} catch(ConnectionFailedException e) {
			System.out.println(e.getMessage() + ".");
		} catch(PortUnreachableException e) {
			System.out.println("port unreachable.");
		}
	}

	private static ModelRenderProgram createProgramFromSPIRV(TextureAtlas atlas) throws IOException {
		return new ModelRenderProgram(
				Shader.fromSPIRV(readIntoBuffer(Path.of("resources/shaders/modelVertexShader.spv")), ShaderType.VERTEX),
				Shader.fromSPIRV(readIntoBuffer(Path.of("resources/shaders/modelFragmentShader.spv")), ShaderType.FRAGMENT),
				atlas.texture()
		);
	}

	@SuppressWarnings("unused")
	private static ModelRenderProgram createProgramFromSource(TextureAtlas atlas) throws IOException {
		return new ModelRenderProgram(
				Shader.fromSource(Files.readString(Path.of("resources/shaders/modelVertexShader")), ShaderType.VERTEX),
				Shader.fromSource(Files.readString(Path.of("resources/shaders/modelFragmentShader")), ShaderType.FRAGMENT),
				atlas.texture()
		);
	}

	private static ByteBuffer readIntoBuffer(Path file) throws IOException {
		ByteBuffer out = ByteBuffer.allocateDirect((int)Files.size(file));
		Files.newByteChannel(file, StandardOpenOption.READ).read(out);
		out.flip();
		return out;
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
