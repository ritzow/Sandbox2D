package ritzow.sandbox.client.graphics;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL46C.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

/** Contains methods for initializing and destroying the OpenGL context and rendering and updating the display **/
public class RenderManager {
	public static final Framebuffer DISPLAY_BUFFER = new Framebuffer(0);

	public static void initializeContext() {
		org.lwjgl.opengl.GL.createCapabilities();
		glEnable(GL_DEBUG_OUTPUT);
		glDebugMessageCallback(RenderManager::debugCallback, 0);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		GraphicsUtility.checkErrors();
		glfwSwapInterval(0);
	}
	
	private static void debugCallback(int source, int type, int id, int severity, 
			int length, long message, long userParam) {
		(severity == GL_DEBUG_SEVERITY_NOTIFICATION ? System.out : System.err)
			.println(MemoryUtil.memASCII(message, length));
	}

	public static void closeContext() {
		GraphicsUtility.checkErrors();
		GLFW.glfwMakeContextCurrent(0);
		org.lwjgl.opengl.GL.destroy();
	}
	
	public static void preRender(int framebufferWidth, int framebufferHeight) {
		glViewport(0, 0, framebufferWidth, framebufferHeight);
	}
}
