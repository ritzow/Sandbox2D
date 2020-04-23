package ritzow.sandbox.client;

import java.io.IOException;
import java.net.NetworkInterface;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.util.Utility;

import static org.lwjgl.glfw.GLFW.*;
import static ritzow.sandbox.client.data.StandardClientProperties.CURSORS_PATH;
import static ritzow.sandbox.client.data.StandardClientProperties.TEXTURES_PATH;
import static ritzow.sandbox.client.util.ClientUtility.log;

/**
 * Entry point to Sandbox2D game client
 **/
class StartClient {

	/**
	 * Native launcher entry point.
	 * @param args command line arguments as a single String.
	 * @throws IOException if an exception occurs during program execution
	 **/
	@SuppressWarnings("unused")
	public static void start(String args) throws IOException {
		run();
	}

	/**
	 * Command line entry point.
	 * @param args command line arguments.
	 * @throws IOException if the program encounters an error.
	 **/
	public static void main(String[] args) throws IOException {
		run();
	}

	private static void run() throws IOException {
		log().info("Starting game");
		long startupStart = System.nanoTime();
		AudioSystem audio = AudioSystem.getDefault(); //load default audio system
		setupGLFW();
		GameState.display().setGraphicsContextOnThread();
		GameState.setShader(RenderManager.setup());
		MainMenuContext mainMenu = new MainMenuContext();
		GameState.setMenuContext(mainMenu);
		log().info("Game startup took " + Utility.formatTime(Utility.nanosSince(startupStart)));
		GameState.display().show();
		try {
			GameLoop.start(mainMenu::update);
		} finally {
			log().info("Exiting game");
			GameState.shader().delete();
			RenderManager.closeContext();
			GameState.display().destroy();
			audio.close();
			glfwDestroyCursor(GameState.cursorPick());
			glfwDestroyCursor(GameState.cursorMallet());
			glfwTerminate();
			log().info("Game exited");
		}
	}

	private static void setupGLFW() throws IOException {
		log().info("Loading GLFW and creating window");
		glfwSetErrorCallback(GLFWErrorCallback.createThrow());
		if(!glfwInit()) throw new RuntimeException("GLFW failed to initialize");
		var appIcon = ClientUtility.loadGLFWImage(TEXTURES_PATH.resolve("redSquare.png"));
		var cursorPickaxe = ClientUtility.loadGLFWImage(CURSORS_PATH.resolve("pickaxe32.png"));
		var cursorMallet = ClientUtility.loadGLFWImage(CURSORS_PATH.resolve("mallet32.png"));
		GameState.setCursorPick(ClientUtility.loadGLFWCursor(cursorPickaxe, 0, 0.66f));
		GameState.setCursorMallet(ClientUtility.loadGLFWCursor(cursorMallet, 0, 0.66f));
		GameState.setDisplay(new Display(4, StandardClientOptions.USE_OPENGL_4_6 ? 6 : 1, "Sandbox2D", appIcon));
	}
}
