package ritzow.sandbox.client.util;

import static org.lwjgl.glfw.GLFW.glfwCreateCursor;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWImage;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.util.Utility;

public class ClientUtility {
	public static long loadGLFWCursor(GLFWImage image, float ratioX, float ratioY) {
		return glfwCreateCursor(image, (int)(image.width() * ratioX), (int)(image.height() * ratioY));
	}
	
	public static GLFWImage loadGLFWImage(Path file) throws IOException {
		PNGDecoder decoder = new PNGDecoder(Files.newInputStream(file));
		ByteBuffer pixels = BufferUtils.createByteBuffer(decoder.getWidth() * decoder.getHeight() * 4);
		decoder.decode(pixels, decoder.getWidth() * 4, Format.RGBA);
		pixels.flip();
		return GLFWImage.create().set(decoder.getWidth(), decoder.getHeight(), pixels);
	}
	
	public static float pixelHorizontalToWorld(Camera camera, int mouseX, int frameWidth, int frameHeight) {
		float worldX = (2f * mouseX) / frameWidth - 1f; //normalize the mouse coordinate
		worldX /= frameHeight/(float)frameWidth; 		//apply aspect ratio
		worldX /= camera.getZoom(); 					//apply zoom
		worldX += camera.getPositionX(); 				//apply camera position
		return worldX;
	}
	
	public static float pixelVerticalToWorld(Camera camera, int mouseY, int framebufferHeight) {
		float worldY = -((2f * mouseY) / framebufferHeight - 1f);
		worldY /= camera.getZoom();
		worldY += camera.getPositionY();
		return worldY;
	}
	
	public static float getViewRightBound(Camera camera, int framebufferWidth, int framebufferHeight) {
		//far right of screen, after accounting for aspect ratio, in world coordinates
		return framebufferWidth/(camera.getZoom() * framebufferHeight) + camera.getPositionX();
	}
	
	public static float getViewLeftBound(Camera camera, int framebufferWidth, int framebufferHeight) {
		//far left of screen after accounting for aspect ratio, in world coordinates
		return getViewRightBound(camera, -framebufferWidth, framebufferHeight);
	}
	
	//width and height for any future need
	public static float getViewTopBound(Camera camera, int framebufferWidth, int framebufferHeight) {
		return 1/camera.getZoom() + camera.getPositionY();
	}
	
	//width and height for any future need
	public static float getViewBottomBound(Camera camera, int framebufferWidth, int framebufferHeight) {
		return -1/camera.getZoom() + camera.getPositionY();
	}
	
	public static void printFrameRate(long frameStart) {
		System.out.print((long)(1/(0.000000001 * Utility.nanosSince(frameStart)))); System.out.println(" FPS");
	}
}
