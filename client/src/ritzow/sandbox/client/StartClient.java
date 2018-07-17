package ritzow.sandbox.client;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
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
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.network.Client.ConnectionFailedException;
import ritzow.sandbox.client.ui.Cursors;
import ritzow.sandbox.client.ui.UserInterfaceRenderer;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.SharedConstants;
import ritzow.sandbox.util.TaskQueue;
import ritzow.sandbox.util.Utility;

public class StartClient {
	private static boolean triggerExit;
	
	public static void main(String[] args) throws IOException {
		Thread.currentThread().setName("Game Thread");
		
		try {
			var serverSocket = Utility.getAddressOrDefault(args, 0, InetAddress.getLocalHost(), Protocol.DEFAULT_SERVER_UDP_PORT);
			var localSocket = Utility.getAddressOrDefault(args, 2, Inet6Address.getLocalHost(), 0);
			
			System.out.print("Connecting to " + Utility.formatAddress(serverSocket) + " from " + Utility.formatAddress(localSocket) + "... ");
			Client client = Client.connect(localSocket, serverSocket);
			System.out.println("connected!");
			
			AudioSystem audio = setupAudio();
			Display display = setupGLFW();
			EventDelegator input = display.getEventDelegator();
			
			//mute audio when window is in background
			input.windowFocusHandlers().add(focused -> audio.setVolume(focused ? 1.0f : 0.0f));
			
			Camera camera = new Camera(0, 0, 1);
			
			var cameraGrip = new TrackingCameraController(camera, audio, client.getPlayer(), 0.005f, 0.05f, 0.6f);
			cameraGrip.link(input);
			
			var playerController = new PlayerController(client);
			playerController.link(input);
			
			var interactionController = new InteractionController(client, 5);
			interactionController.link(input);
			
			input.keyboardHandlers().add((key, scancode, action, mods) -> {
				if(action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
					if(key == ControlScheme.KEYBIND_QUIT)
						triggerExit = true;
					else if(key == ControlScheme.KEYBIND_FULLSCREEN)
			            display.toggleFullscreen();
				}
			});

			input.windowCloseHandlers().add(() -> triggerExit = true);
			
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
			
			renderer.getRenderers().add(new ClientWorldRenderer(modelProgram, cameraGrip.getCamera(), world));
			
			var ui = new UserInterfaceRenderer(modelProgram);
			//renderer.getRenderers().add(ui);
			
			//display the window now that everything is set up.
			display.show();
			
			TaskQueue queue = new TaskQueue();
			client.setExecutor(queue::add);
			long lastUpdate = System.nanoTime();
			try {
				while(!triggerExit && client.isConnected()) {
					glfwPollEvents(); //process input/windowing events
					queue.run();
					lastUpdate = display.focused() ? Utility.updateWorld(world, lastUpdate, 
							SharedConstants.MAX_TIMESTEP, SharedConstants.TIME_SCALE_NANOSECONDS) : System.nanoTime();
					cameraGrip.update();
					interactionController.update(camera, display.width(), display.height());
					if(display.focused()) {
						ui.update();
						renderer.run(display);
					}
					Utility.sleep(1);
				}
			} finally {
				System.out.print("Exiting... ");
				renderer.close(display);
				display.destroy();
				if(client.isConnected())
					client.disconnect();
				audio.close();
				glfwTerminate();
				System.out.println("done!");
			}
		} catch(ConnectionFailedException e) {
			System.out.println(e.getMessage() + ".");
		}
	}
	
	private static Display setupGLFW() throws IOException {
		if(!glfwInit())
			throw new UnsupportedOperationException("GLFW failed to initialize");
		GLFWErrorCallback.createPrint(System.err).set();
		Display display = new Display("Sandbox2D", 4, 5);
		display.setCursor(Cursors.load(Path.of("resources/assets/textures/cursors/pickaxe32.png"), 0, 0.66f));
		return display;
	}
	
	private static AudioSystem setupAudio() {
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
			try {
				SoundInfo info = WAVEDecoder.decode(Files.newInputStream(directory.resolve(entry.getKey())));
				audio.registerSound(entry.getValue().code(), info);
			} catch(NoSuchFileException e) {
				System.out.println("The file " + e.getFile() + " does not exist");
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		return audio;
	}
	
	private static void register(ModelRenderProgram prog, int id, int indices, int positions, TextureAtlas atlas, TextureData data) {
		prog.register(id, new RenderData(6, indices, positions, GraphicsUtility.uploadVertexData(atlas.getCoordinates(data))));
	}
}
