package resource;

import static org.lwjgl.glfw.GLFW.glfwCreateCursor;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import java.io.*;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWImage;

public class Cursors {
	public static long LEAFY_STICK;
	
	public static void loadAll() {
		try {
			LEAFY_STICK = Cursors.loadCursor(new FileInputStream(new File("resources/assets/textures/cursor.png")), true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static long loadCursor(InputStream image, boolean cursorLocationTopLeft) throws IOException {
		GLFWImage cursorImage = GLFWImage.create();
		PNGDecoder decoder = new PNGDecoder(image);
		ByteBuffer pixels = BufferUtils.createByteBuffer(decoder.getWidth() * decoder.getHeight() * 4);
		decoder.decode(pixels, decoder.getWidth() * 4, Format.RGBA);
		pixels.flip();
		cursorImage.set(decoder.getWidth(), decoder.getHeight(), pixels);
		
		if(cursorLocationTopLeft) {
			return glfwCreateCursor(cursorImage, 0, 0);
		} 
		
		else {
			return glfwCreateCursor(cursorImage, decoder.getWidth()/2, decoder.getHeight()/2);
		}
	}
}
