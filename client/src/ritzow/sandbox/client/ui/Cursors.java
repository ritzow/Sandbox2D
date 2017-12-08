package ritzow.sandbox.client.ui;

import static org.lwjgl.glfw.GLFW.glfwCreateCursor;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWImage;

public final class Cursors {
	
	public static long PICKAXE;
	
	public static void loadAll() throws IOException {
		Cursors.PICKAXE = loadCursor(new File("resources/assets/textures/cursors/pickaxe32.png"), 0, 0.66f);
	}
	
	public static long loadCursor(File image, float ratioX, float ratioY) throws IOException {
		PNGDecoder decoder = new PNGDecoder(new FileInputStream(image));
		ByteBuffer pixels = BufferUtils.createByteBuffer(decoder.getWidth() * decoder.getHeight() * 4);
		decoder.decode(pixels, decoder.getWidth() * 4, Format.RGBA);
		pixels.flip();
		GLFWImage cursorImage = GLFWImage.create().set(decoder.getWidth(), decoder.getHeight(), pixels);
		return glfwCreateCursor(cursorImage, (int)(decoder.getWidth() * ratioX), (int)(decoder.getHeight() * ratioY));
	}
}
