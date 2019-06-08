package ritzow.sandbox.client;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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
	private static final long UPDATE_SKIP_THRESHOLD_NANOSECONDS = Utility.millisToNanos(100);
	private static final boolean USE_OPENGL_4_6 = false;

	private static Display display;
	private static long pickaxeCursor;
	private static EventDelegator input;
	private static AudioSystem audio;
	private static RenderManager renderer;
	private static ModelRenderProgram shaderProgram;
	private static InetSocketAddress localAddress, serverAddress;
	private static GameState state;
	private static boolean quit;
	
	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("Game Loop");
		setAddresses();
		setupInitial();
		GameLoop.run(StartClient::menuLoop);
		System.out.println("done!");
	}
	
	private static void setAddresses(String... args) throws NumberFormatException, UnknownHostException, SocketException {
		serverAddress =	Utility.getAddressOrDefault(args, 0, InetAddress.getLocalHost(), Protocol.DEFAULT_SERVER_PORT_UDP);
		localAddress = new InetSocketAddress(Utility.getPublicAddress(Inet4Address.class), 0);
	}
	
	private static class GameState {
		UpdateClient client;
		ClientPlayerEntity player;
		World world;
		TrackingCameraController cameraGrip;
		PlayerController playerControls;
		InteractionController interactionControls;
		long lastWorldUpdate, lastCameraUpdate;
		
		void connect() throws NumberFormatException, IOException {
			System.out.print("Connecting to " + Utility.formatAddress(serverAddress)
			+ " from " + Utility.formatAddress(localAddress) + "... ");
			client = UpdateClient.startConnection(localAddress, serverAddress);
			client.setEventListener(ClientEvent.CONNECT_ACCEPTED, this::onAccepted);
			client.setEventListener(ClientEvent.CONNECT_REJECTED, this::onRejected);
			client.setEventListener(ClientEvent.DISCONNECTED, this::onDisconnected);
			client.setEventListener(ClientEvent.WORLD_JOIN, this::setupGameWorld);
			GameLoop.set(client::update);
		}
		
		void setupGameWorld(World world, ClientPlayerEntity player) {
			this.world = world;
			this.player = player;
			cameraGrip = new TrackingCameraController(audio, player, 2.5f, 0.05f, 0.6f);
			cameraGrip.link(input);
			renderer.getRenderers().add(new ClientWorldRenderer(shaderProgram, cameraGrip.getCamera(), world));
			playerControls = new PlayerController(client);
			playerControls.link(input);
			interactionControls = new InteractionController(client);
			interactionControls.link(input);
			display.setCursor(pickaxeCursor);
			lastWorldUpdate = System.nanoTime();
			lastCameraUpdate = System.nanoTime();
			GameLoop.set(this::updateGame);
		}
		
		void updateGame() throws TimeoutException, IOException {
			glfwPollEvents(); //process input/windowing events
			client.update();
			lastWorldUpdate = updateWorld(display, world, lastWorldUpdate); //simulate world on client
			long camUpdateStart = System.nanoTime();
			cameraGrip.update(camUpdateStart - lastCameraUpdate); //update camera position and zoom
			lastCameraUpdate = camUpdateStart;
			interactionControls.update(cameraGrip.getCamera(), client, 
					world, player, display.width(), display.height()); //block breaking/"bomb throwing"
			if(!display.minimized()) renderer.run(display);
			Utility.sleep(1); //reduce CPU usage
		}
		
		private void onAccepted() {
			System.out.println("connected.");
		}
		
		private void onRejected() {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			returnToMenu();
		}
		
		private void onDisconnected() {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(display.wasClosed()) {
				StartClient.exit();
			} else {
				returnToMenu();
			}
		}
		
		private void exit() {
			client.startDisconnect();
			GameLoop.set(client::update);
		}
	}
	
	private static void returnToMenu() {
		state = null;
		display.resetCursor();
		renderer.getRenderers().clear();
		renderer.getRenderers().add(new MenuRenderer(shaderProgram));
		GameLoop.set(StartClient::menuLoop);	
	}
	
	//TODO need to refactor StartClient again, split GameState, MenuState, etc. into different separate contexts
	//keeping in mind UI overlays, etc. in-game
	private static void menuLoop() {
		glfwPollEvents(); //process input/windowing events
		if(!quit) {
			renderer.run(display);
			Utility.sleep(1); //reduce CPU usage	
		}
	}
	
	private static void exit() {
		quit = true;
		GameLoop.set(); //stop the game loop now that the client is closed
		System.out.print("Exiting... ");
		shaderProgram.delete();
		renderer.close(display);
		display.destroy();
		audio.close();
		glfwTerminate();
	}
	
	private static void quit() {
		if(state == null) {
			exit();
		} else {
			state.exit();
		}
	}
	
	private static void setupInitial() throws IOException {
		long startupStart = System.nanoTime();
		audio = setupAudio();
		display = setupGLFW();
		input = display.getEventDelegator();
		shaderProgram = setupRenderer(display);

		//mute audio when window is in background
		input.windowFocusHandlers().add(focused -> audio.setVolume(focused ? 1.0f : 0.0f));
		input.windowCloseHandlers().add(StartClient::quit);
		input.keyboardHandlers().add((key, scancode, action, mods) -> {
			if(action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
				switch(key) {
					case ControlScheme.KEYBIND_CONNECT -> {
						if(state == null) {
							try {
								(state = new GameState()).connect();
							} catch (NumberFormatException | IOException e) {
								e.printStackTrace();
							}
						}
					}
					case ControlScheme.KEYBIND_QUIT -> quit();
					case ControlScheme.KEYBIND_FULLSCREEN -> display.toggleFullscreen();
				}
			}
		});
		
		renderer.getRenderers().add(new MenuRenderer(shaderProgram));
		
		//display the window now that everything is set up.
		display.show();
		System.out.println("Startup took " + Utility.formatTime(Utility.nanosSince(startupStart)));
	}
	
	private static ModelRenderProgram setupRenderer(Display display) throws IOException {
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
		
		return program;
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
		pickaxeCursor = ClientUtility.loadGlfwCursor(cursor, 0, 0.66f);
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
