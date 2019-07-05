package ritzow.sandbox.client;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwDestroyCursor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.audio.DefaultAudioSystem;
import ritzow.sandbox.client.audio.OpenALAudioSystem;
import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.client.audio.SoundInfo;
import ritzow.sandbox.client.audio.WAVEDecoder;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;

public class StartClient {
	static final long UPDATE_SKIP_THRESHOLD_NANOSECONDS = Utility.millisToNanos(100);
	private static final boolean USE_OPENGL_4_6 = false;

	static Display display;
	static long pickaxeCursor;
	static AudioSystem audio;
	static ModelRenderProgram shaderProgram;
	static InetSocketAddress localAddress;
	static InetSocketAddress serverAddress;
	static MainMenuContext mainMenu;

	/** For use by native launcher **/
	public static void start(String commandLine) throws Exception {
		main(commandLine.split(" "));
	}

	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("Game Loop");
		long startupStart = System.nanoTime();
		InetAddress localHost = NetworkUtility.getLoopbackAddress();
		serverAddress =	Utility.getAddressFromProgramArgumentsOrDefault(args, 0, localHost, Protocol.DEFAULT_SERVER_PORT_UDP);
		localAddress = new InetSocketAddress(serverAddress.getAddress().equals(localHost) ? localHost : NetworkUtility.getPrimaryAddress(), 0);
		audio = setupAudio();
		display = setupGLFW();
		shaderProgram = setupGraphics(display);
		mainMenu = new MainMenuContext(shaderProgram);
		System.out.println("Startup took " + Utility.formatTime(Utility.nanosSince(startupStart)));
		display.show();
		GameLoop.start(mainMenu);
		System.out.println("done!");
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

		int indices = GraphicsUtility.uploadIndexData(0, 1, 2, 0, 2, 3);

		int positions = GraphicsUtility.uploadVertexData(
				-0.5f,	 0.5f,
				-0.5f,	-0.5f,
				0.5f,	-0.5f,
				0.5f,	 0.5f
		);

		TextureData
			dirt = Textures.loadTextureName("dirt"),
			grass = Textures.loadTextureName("grass"),
			face = Textures.loadTextureName("greenFace"),
			red = Textures.loadTextureName("redSquare"),
			sky = Textures.loadTextureName("clouds");
		TextureAtlas atlas = Textures.buildAtlas(grass, dirt, face, red, sky);

		ModelRenderProgram program = USE_OPENGL_4_6 ? createProgramFromSPIRV(atlas) : createProgramFromSource(atlas);
		GraphicsUtility.checkErrors();

		program.register(RenderConstants.MODEL_DIRT_BLOCK, getRenderData(indices, positions, atlas, dirt));
		program.register(RenderConstants.MODEL_GRASS_BLOCK, getRenderData(indices, positions, atlas, grass));
		program.register(RenderConstants.MODEL_GREEN_FACE, getRenderData(indices, positions, atlas, face));
		program.register(RenderConstants.MODEL_RED_SQUARE, getRenderData(indices, positions, atlas, red));
		program.register(RenderConstants.MODEL_SKY, getRenderData(indices, positions, atlas, sky));

		return program;
	}

	private static RenderData getRenderData(int indices, int positions, TextureAtlas atlas, TextureData data) {
		return new RenderData(6, indices, positions, GraphicsUtility.uploadVertexData(atlas.getCoordinates(data)));
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

	private static Display setupGLFW() throws IOException {
		if(!glfwInit())
			throw new RuntimeException("GLFW failed to initialize");
		glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err));
		GLFWImage icon = ClientUtility.loadGLFWImage(Path.of("resources/assets/textures/redSquare.png"));
		Display display = new Display(4, 5, "Sandbox2D", icon);
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
				SoundInfo info = WAVEDecoder.decode(in);
				audio.registerSound(entry.getValue().code(), info);
			}
		}
		return audio;
	}
}
