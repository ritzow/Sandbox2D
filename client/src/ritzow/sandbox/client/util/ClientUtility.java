package ritzow.sandbox.client.util;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWImage;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.util.Utility;

import static org.lwjgl.glfw.GLFW.glfwCreateCursor;

public class ClientUtility {
	private static final Logger LOGGER;

	static {
		LOGGER = Logger.getLogger("game");
		LOGGER.setUseParentHandlers(false);
		LOGGER.addHandler(new Handler() {
			@Override
			public void publish(LogRecord record) {
				(record.getLevel().intValue() >= Level.WARNING.intValue() ? System.err : System.out)
						.println(Utility.formatCurrentTime() + " " + record.getMessage());
			}

			@Override
			public void flush() {
				System.out.flush();
				System.err.flush();
			}

			@Override
			public void close() throws SecurityException {}
		});
	}

	public static Logger log() {
		return LOGGER;
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

	public static float scaleToWidth(float width, Model model) {
		return width/model.width();
	}

	public static float scaleToHeight(float height, Model model) {
		return height/model.height();
	}

	public static float scaleToFit(float width, float height, Model model) {
		//https://stackoverflow.com/questions/1373035/how-do-i-scale-one-rectangle-to-the-maximum-size-possible-within-another-rectang
		float h = width/model.width();
		float v = height/model.height();
		return h <= v ? h : v;
	}

	public static float scaleToFit(float widthNew, float heightNew, float widthOriginal, float heightOriginal) {
		//https://stackoverflow.com/questions/1373035/how-do-i-scale-one-rectangle-to-the-maximum-size-possible-within-another-rectang
		float h = widthNew/widthOriginal;
		float v = heightNew/heightOriginal;
		return h <= v ? h : v;
	}

	private static final Logger FPS_LOGGER = Logger.getLogger("framerate");

	public static void logFrameRate(long frameStart) {
		FPS_LOGGER.info(1_000_000_000 * Utility.nanosSince(frameStart) + " FPS");
	}
}
