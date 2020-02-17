package ritzow.sandbox.client;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwDestroyCursor;
import static ritzow.sandbox.client.data.StandardClientProperties.*;

import java.io.IOException;
import org.lwjgl.glfw.GLFWErrorCallback;
import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.audio.DefaultAudioSystem;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.util.Utility;

/** Entry point to Sandbox2D game client **/
public class StartClient {
	
	/** Native launcher entry point.
	 * @param args command line arguments as a single String.
	 * @throws Exception if an exception occurrs during program execution **/
	public static void start(String args) throws Exception {
		start();
	}
	
	/** Command line entry point.
	 * @param args command line arguments.
	 * @throws Exception if the program encounters an error. **/
	public static void main(String[] args) throws Exception {
		start();
	}
	
	private static void start() throws Exception {
		long startupStart = System.nanoTime();
		System.out.print("Starting game... ");
		AudioSystem.load();
		GameState state = new GameState();
		setupGLFW(state);
		state.display.setGraphicsContextOnThread();
		state.shader = RenderManager.setup();
		MainMenuContext mainMenu = new MainMenuContext(state);
		System.out.println("took " + Utility.formatTime(Utility.nanosSince(startupStart)));
		state.display.show();
		try {
			GameLoop.start(mainMenu::update);
		} finally {
			System.out.print("Exiting... ");
			state.shader.delete();
			RenderManager.closeContext();
			state.display.destroy();
			DefaultAudioSystem.getDefault().close();
			glfwDestroyCursor(state.cursorPick);
			glfwDestroyCursor(state.cursorMallet);
			glfwTerminate();
			System.out.println("done!");	
		}
	}

	private static void setupGLFW(GameState state) throws IOException {
		glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err));
		if(!glfwInit()) throw new RuntimeException("GLFW failed to initialize");
		var appIcon   = ClientUtility.loadGLFWImage(TEXTURES_PATH.resolve("redSquare.png"));
		var cursorPickaxe = ClientUtility.loadGLFWImage(CURSORS_PATH.resolve("pickaxe32.png"));
		var cursorMallet = ClientUtility.loadGLFWImage(CURSORS_PATH.resolve("mallet32.png"));
		state.cursorPick = ClientUtility.loadGLFWCursor(cursorPickaxe, 0, 0.66f);
		state.cursorMallet = ClientUtility.loadGLFWCursor(cursorMallet, 0, 0.66f);
		state.display = new Display(4, StandardClientOptions.USE_OPENGL_4_6 ? 6 : 1, "Sandbox2D", appIcon);
	}
}
