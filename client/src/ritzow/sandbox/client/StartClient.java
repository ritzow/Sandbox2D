package ritzow.sandbox.client;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwDestroyCursor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.audio.DefaultAudioSystem;
import ritzow.sandbox.client.audio.OpenALAudioSystem;
import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.client.audio.WAVEDecoderNIO;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.graphics.ModelRenderProgram.ModelData;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.util.Utility;

public class StartClient {
	static Display display;
	static long pickaxeCursor, malletCursor;
	static AudioSystem audio;
	static ModelRenderProgram shaderProgram;
	static InetSocketAddress localAddress, serverAddress;
	static MainMenuContext mainMenu;
	
	/** For use by native launcher **/
	public static void start(String args) throws Exception {
		main(Pattern
			.compile("\\S+") //Pattern: all character sequences split by whitespace
			.matcher(args)
			.results()
			.map(MatchResult::group)
			.toArray(String[]::new)
		);
	}
	
	public static void main(String[] args) throws Exception {
		long startupStart = System.nanoTime();
		System.out.print("Starting game... ");
		audio = setupAudio();
		display = setupGLFW();
		shaderProgram = setupGraphics(display);
		mainMenu = new MainMenuContext(shaderProgram);
		localAddress = StandardClientOptions.LAST_LOCAL_ADDRESS;
		serverAddress = StandardClientOptions.LAST_SERVER_ADDRESS;
		System.out.println("took " + Utility.formatTime(Utility.nanosSince(startupStart)));
		System.out.println(NetworkUtility.formatAddress(localAddress) + " → " + NetworkUtility.formatAddress(serverAddress));
		display.show();
		GameLoop.start(mainMenu::update);
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
		glfwDestroyCursor(malletCursor);
		glfwTerminate();
	}

	//only takes about 23 milliseconds out of entire startup time
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
			sky = Textures.loadTextureName("clouds_online");
		TextureAtlas atlas = Textures.buildAtlas(sky, grass, dirt, face, red);
		
		//TODO look into using https://github.com/javagl/JglTF with Blender
		ModelData[] models = {
			new ModelData(RenderConstants.MODEL_DIRT_BLOCK, positions, atlas.getCoordinates(dirt), indices),
			new ModelData(RenderConstants.MODEL_GRASS_BLOCK, positions, atlas.getCoordinates(grass), indices),
			new ModelData(RenderConstants.MODEL_GREEN_FACE, positions, atlas.getCoordinates(face), indices),
			new ModelData(RenderConstants.MODEL_RED_SQUARE, positions, atlas.getCoordinates(red), indices),
			new ModelData(RenderConstants.MODEL_SKY, positions, atlas.getCoordinates(sky), indices)
		};
		
		Shader vertex = StandardClientOptions.USE_OPENGL_4_6 ? 
			spirv("resources/shaders/model.vert.spv", ShaderType.VERTEX) :
			source("resources/shaders/newmodel.vert", ShaderType.VERTEX);
		Shader fragment = StandardClientOptions.USE_OPENGL_4_6 ? 
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
		glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err));
		if(!glfwInit()) throw new RuntimeException("GLFW failed to initialize");
		GLFWImage icon = ClientUtility.loadGLFWImage(Path.of("resources/assets/textures/redSquare.png"));
		var cursor = ClientUtility.loadGLFWImage(Path.of("resources/assets/textures/cursors/pickaxe32.png"));
		var mallet = ClientUtility.loadGLFWImage(Path.of("resources/assets/textures/cursors/mallet32.png"));
		pickaxeCursor = ClientUtility.loadGLFWCursor(cursor, 0, 0.66f);
		malletCursor = ClientUtility.loadGLFWCursor(mallet, 0, 0.66f);
		return new Display(4, StandardClientOptions.USE_OPENGL_4_6 ? 6 : 1, true, "Sandbox2D", icon);
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
			audio.registerSound(entry.getValue().code(), WAVEDecoderNIO.decode(directory.resolve(entry.getKey())));
		}
		return audio;
	}
}
