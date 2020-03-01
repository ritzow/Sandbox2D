package ritzow.sandbox.client;

import java.io.IOException;
import org.lwjgl.glfw.GLFWErrorCallback;
import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.util.Utility;

import static org.lwjgl.glfw.GLFW.*;
import static ritzow.sandbox.client.data.StandardClientProperties.CURSORS_PATH;
import static ritzow.sandbox.client.data.StandardClientProperties.TEXTURES_PATH;
import static ritzow.sandbox.client.util.ClientUtility.log;

/** Entry point to Sandbox2D game client **/
class StartClient {

	/** Native launcher entry point.
	 * @param args command line arguments as a single String.
	 * @throws Exception if an exception occurs during program execution **/
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
		log().info("Starting game");
		long startupStart = System.nanoTime();
		AudioSystem audio = AudioSystem.getDefault(); //load default audio system
		GameState state = new GameState();
		setupGLFW(state);
		state.display.setGraphicsContextOnThread();
		state.shader = RenderManager.setup();
		MainMenuContext mainMenu = new MainMenuContext(state);
		log().info("Game startup took " + Utility.formatTime(Utility.nanosSince(startupStart)));
		state.display.show();
		try {
			GameLoop.start(mainMenu::update);
		} finally {
			log().info("Exiting game");
			state.shader.delete();
			RenderManager.closeContext();
			state.display.destroy();
			audio.close();
			glfwDestroyCursor(state.cursorPick);
			glfwDestroyCursor(state.cursorMallet);
			glfwTerminate();
			log().info("Game exited");
		}
	}

	private static void setupGLFW(GameState state) throws IOException {
		log().info("Loading GLFW and creating window");
		glfwSetErrorCallback(GLFWErrorCallback.createThrow());
		if(!glfwInit()) throw new RuntimeException("GLFW failed to initialize");
		var appIcon   = ClientUtility.loadGLFWImage(TEXTURES_PATH.resolve("redSquare.png"));
		var cursorPickaxe = ClientUtility.loadGLFWImage(CURSORS_PATH.resolve("pickaxe32.png"));
		var cursorMallet = ClientUtility.loadGLFWImage(CURSORS_PATH.resolve("mallet32.png"));
		state.cursorPick = ClientUtility.loadGLFWCursor(cursorPickaxe, 0, 0.66f);
		state.cursorMallet = ClientUtility.loadGLFWCursor(cursorMallet, 0, 0.66f);
		state.display = new Display(4, StandardClientOptions.USE_OPENGL_4_6 ? 6 : 1, "Sandbox2D", appIcon);
	}
}
