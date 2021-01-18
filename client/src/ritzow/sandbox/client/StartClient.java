package ritzow.sandbox.client;

import java.io.IOException;
import java.util.Set;
import net.straylightlabs.hola.dns.Domain;
import net.straylightlabs.hola.sd.Instance;
import net.straylightlabs.hola.sd.Query;
import net.straylightlabs.hola.sd.Service;
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

/**
 * Entry point to Sandbox2D game client
 **/
class StartClient {

	/**
	 * Command line entry point.
	 * @param args command line arguments.
	 * @throws IOException if the program encounters an error.
	 **/
	public static void main(String[] args) throws IOException {

		System.out.println(Service.fromName("_0hello._hithere._udp"));

		Query query = Query.createFor(Service.fromName("_services._dns-sd._udp"), Domain.LOCAL);



		Set<Instance> results = query.runOnce();



		System.out.println(results);

		System.exit(0);

		log().info("Starting game");
		long startupStart = System.nanoTime();
		AudioSystem.getDefault(); //load default audio system
		setupGLFW();
		RenderManager.setup();
		GameState.setLightRenderer(RenderManager.LIGHT_RENDERER);
		GameState.modelRenderer(RenderManager.MODEL_RENDERER);
		MainMenuContext mainMenu = new MainMenuContext();
		GameState.setMenuContext(mainMenu);
		log().info("Game startup took " + Utility.formatTime(Utility.nanosSince(startupStart)));
		GameState.display().show();
		GameLoop.start(mainMenu::update);
	}

	public static void shutdown() {
		log().info("Exiting game");
		GameState.modelRenderer().delete();
		RenderManager.closeContext();
		GameState.display().destroy();
		AudioSystem.getDefault().close();
		glfwDestroyCursor(GameState.cursorPick());
		glfwDestroyCursor(GameState.cursorMallet());
		glfwTerminate();
		log().info("Game exited");
		GameLoop.stop();
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
		GameState.setDisplay(new Display(4, StandardClientOptions.USE_OPENGL_4_6 ? 6 : 3, "Sandbox2D", appIcon));
		RenderManager.OPENGL_CAPS = GameState.display().setGraphicsContextOnThread();
	}
}
