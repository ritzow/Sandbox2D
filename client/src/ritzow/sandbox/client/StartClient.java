package ritzow.sandbox.client;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwDestroyCursor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.regex.Pattern;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.audio.DefaultAudioSystem;
import ritzow.sandbox.client.audio.OpenALAudioSystem;
import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.client.audio.WAVEDecoder;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.graphics.ModelRenderProgram.ModelData;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;

public class StartClient {
	public static final boolean 
		USE_OPENGL_4_6 = false, 
		LIMIT_FPS = true, 
		PRINT_FPS = false;

	public static final long 
		FRAME_RATE_LIMIT = 120,
		FRAME_TIME_LIMIT = ClientUtility.frameRateToFrameTimeNanos(FRAME_RATE_LIMIT),
		UPDATE_SKIP_THRESHOLD_NANOSECONDS = Utility.millisToNanos(100);

	static Display display;
	static long pickaxeCursor;
	static AudioSystem audio;
	static ModelRenderProgram shaderProgram;
	static InetSocketAddress localAddress, serverAddress;
	static MainMenuContext mainMenu;
	
	/** For use by native launcher **/
	public static void start(String args) throws Exception {
		//Pattern: all character sequences split by whitespace
		String[] formatted = Pattern
			.compile("\\S+")
			.matcher(args)
			.results()
			.map(result -> result.group())
			.toArray(length -> new String[length]);
		main(formatted);
	}
	
	public static void main(String[] args) throws Exception {
		long startupStart = System.nanoTime();
		System.out.print("Starting game... ");
		audio = setupAudio();
		display = setupGLFW();
		shaderProgram = setupGraphics(display);
		mainMenu = new MainMenuContext(shaderProgram);
		configureAddresses(args);
		System.out.println("took " + Utility.formatTime(Utility.nanosSince(startupStart)));
		System.out.println("Local Address: " + Utility.formatAddress(localAddress));
		System.out.println("Server Address: " + Utility.formatAddress(serverAddress));
		display.show();
		GameLoop.start(mainMenu::update);
		System.out.println("done!");
	}
	
	private static void configureAddresses(String[] args) throws SocketException, UnknownHostException {
		if(args.length > 0) {
			switch(args[0]) {
				case "local" -> {
					localAddress = new InetSocketAddress(NetworkUtility.getLoopbackAddress(), 0);
					serverAddress = new InetSocketAddress(NetworkUtility.getLoopbackAddress(), 
							Protocol.DEFAULT_SERVER_PORT_UDP);
				}
				
				case "public" -> {
					serverAddress = new InetSocketAddress(InetAddress.getByName(args[1]), 
							Protocol.DEFAULT_SERVER_PORT_UDP);
					localAddress = NetworkUtility.socketAddress(
							NetworkUtility.getPrimaryAddress(
							NetworkUtility.isIPv6(serverAddress.getAddress())));
				}
				
				default -> throw new UnsupportedOperationException("Unknown address type");
			}
		} else {
			localAddress = new InetSocketAddress(NetworkUtility.getLoopbackAddress(), 0);
			serverAddress = new InetSocketAddress(NetworkUtility.getLoopbackAddress(), 
					Protocol.DEFAULT_SERVER_PORT_UDP);
		}
	}
	
	static void exit() {
		GameLoop.stop(); //stop the game loop now that the client is closed
		System.out.print("Exiting... ");
		shaderProgram.delete();
		RenderManager.closeContext();
		display.destroy();
		audio.close();
		glfwDestroyCursor(pickaxeCursor);
		glfwTerminate();
	}

	private static ModelRenderProgram setupGraphics(Display display) throws IOException {
		display.setGraphicsContextOnThread();
		RenderManager.initializeContext();

		int[] indices = {0, 1, 2, 0, 2, 3};

		float[] positions = {
			-0.5f,	 0.5f,
			-0.5f,	-0.5f,
			0.5f,	-0.5f,
			0.5f,	 0.5f
		};

		TextureData
			dirt = Textures.loadTextureName("dirt"),
			grass = Textures.loadTextureName("grass"),
			face = Textures.loadTextureName("greenFace"),
			red = Textures.loadTextureName("redSquare"),
			sky = Textures.loadTextureName("clouds");
		
		TextureAtlas atlas = Textures.buildAtlas(sky, grass, dirt, face, red);
		
		ModelData[] models = {
			new ModelData(RenderConstants.MODEL_DIRT_BLOCK, positions, atlas.getCoordinates(dirt), indices),
			new ModelData(RenderConstants.MODEL_GRASS_BLOCK, positions, atlas.getCoordinates(grass), indices),
			new ModelData(RenderConstants.MODEL_GREEN_FACE, positions, atlas.getCoordinates(face), indices),
			new ModelData(RenderConstants.MODEL_RED_SQUARE, positions, atlas.getCoordinates(red), indices),
			new ModelData(RenderConstants.MODEL_SKY, positions, atlas.getCoordinates(sky), indices)
		};
		
		Shader vertex = USE_OPENGL_4_6 ? 
			spirv("resources/shaders/model.vert.spv", ShaderType.VERTEX) :
			source("resources/shaders/newmodel.vert", ShaderType.VERTEX);
		Shader fragment = USE_OPENGL_4_6 ? 
			spirv("resources/shaders/model.frag.spv", ShaderType.FRAGMENT) :
			source("resources/shaders/newmodel.frag", ShaderType.FRAGMENT);
			
		return ModelRenderProgram.create(vertex, fragment, atlas.texture(), models);
	}
	
	private static Shader source(String file, ShaderType type) throws IOException {
		return Shader.fromSource(Files.readString(Path.of(file)), type);
	}
	
	private static Shader spirv(String file, ShaderType type) throws IOException {
		try(FileChannel reader = FileChannel.open(Path.of(file), StandardOpenOption.READ)) {
			ByteBuffer dest = BufferUtils.createByteBuffer((int)reader.size());
			reader.read(dest);
			return Shader.fromSPIRV(dest.flip(), type);	
		}
	}

	private static Display setupGLFW() throws IOException {
		if(!glfwInit())
			throw new RuntimeException("GLFW failed to initialize");
		glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err));
		GLFWImage icon = ClientUtility.loadGLFWImage(Path.of("resources/assets/textures/redSquare.png"));
		Display display = new Display(4, USE_OPENGL_4_6 ? 6 : 1, true, "Sandbox2D", icon);
		var cursor = ClientUtility.loadGLFWImage(Path.of("resources/assets/textures/cursors/pickaxe32.png"));
		pickaxeCursor = ClientUtility.loadGLFWCursor(cursor, 0, 0.66f);
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
				audio.registerSound(entry.getValue().code(), WAVEDecoder.decode(in));
			}
		}
		return audio;
	}
}
