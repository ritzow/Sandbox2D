package ritzow.sandbox.client.util;

import static org.lwjgl.glfw.GLFW.glfwCreateCursor;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.LockSupport;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWImage;
import static ritzow.sandbox.client.data.StandardClientOptions.*;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.util.Utility;

public class ClientUtility {
	public static void limitFramerate(long frameStart) {
		if(LIMIT_FPS) LockSupport.parkNanos(FRAME_TIME_LIMIT + frameStart - System.nanoTime());
	}
	
	public static long frameRateToFrameTimeNanos(long fps) {
		return 1_000_000_000/fps;
	}
	
	public static ByteBuffer load(Path file) throws IOException {
		try(FileChannel reader = FileChannel.open(file, StandardOpenOption.READ)) {
			ByteBuffer dest = BufferUtils.createByteBuffer((int)reader.size());
			reader.read(dest);
			return dest.flip();
		}
	}

	public static long loadGLFWCursor(GLFWImage image, int hotspotX, int hotspotY) {
		return glfwCreateCursor(image, hotspotX, hotspotY);
	}
	
	public static long loadGLFWCursor(GLFWImage image, float ratioX, float ratioY) {
		return glfwCreateCursor(image, (int)(image.width() * ratioX), (int)(image.height() * ratioY));
	}
	
	public static GLFWImage loadGLFWImage(Path file) throws IOException {
		PNGDecoder decoder = new PNGDecoder(Files.newInputStream(file));
		int stride = decoder.getWidth() * 4;
		ByteBuffer pixels = BufferUtils.createByteBuffer(stride * decoder.getHeight());
		decoder.decode(pixels, stride, Format.RGBA);
		return GLFWImage.create().set(decoder.getWidth(), decoder.getHeight(), pixels.flip());
	}
	
	public static float pixelHorizontalToWorld(Camera camera, int mouseX, int frameWidth, int frameHeight) {
		float worldX = (2f * mouseX) / frameWidth - 1f; //normalize the mouse coordinate
		worldX *= (float)frameWidth/frameHeight; 		//apply aspect ratio
		worldX /= camera.getZoom(); 					//apply zoom
		worldX += camera.getPositionX(); 				//apply camera position
		return worldX;
	}
	
	public static float pixelVerticalToWorld(Camera camera, int mouseY, int framebufferHeight) {
		return (1f - (2f * mouseY) / framebufferHeight) / camera.getZoom() + camera.getPositionY();
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
	@SuppressWarnings("unused")
	public static float getViewTopBound(Camera camera, int framebufferWidth, int framebufferHeight) {
		return 1f / camera.getZoom() + camera.getPositionY();
	}
	
	//width and height for any future need
	@SuppressWarnings("unused")
	public static float getViewBottomBound(Camera camera, int framebufferWidth, int framebufferHeight) {
		return -1f / camera.getZoom() + camera.getPositionY();
	}
	
	public static void printFrameRate(long frameStart) {
		System.out.print(1_000_000_000 * Utility.nanosSince(frameStart)); 
		System.out.println(" FPS");
	}
}
