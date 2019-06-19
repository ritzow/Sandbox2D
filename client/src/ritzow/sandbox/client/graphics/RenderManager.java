package ritzow.sandbox.client.graphics;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL46C.*;

import org.lwjgl.glfw.GLFW;

/** Contains methods for initializing and destroying the OpenGL context and rendering and updating the display **/
public class RenderManager {
	private static final Framebuffer displayBuffer = new Framebuffer(0);

	public static void initializeContext() {
		org.lwjgl.opengl.GL.createCapabilities();
		glfwSwapInterval(0);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		GraphicsUtility.checkErrors();
	}

	public static void closeContext() {
		GraphicsUtility.checkErrors();
		GLFW.glfwMakeContextCurrent(0);
		org.lwjgl.opengl.GL.destroy();
	}

	public static void run(Display display, Renderer renderer) {
		int width = display.width(), height = display.height();
		glViewport(0, 0, width, height);
		renderer.render(displayBuffer, width, height);
		display.refresh();
		GraphicsUtility.checkErrors();
	}
}
