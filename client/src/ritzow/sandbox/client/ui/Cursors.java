package ritzow.sandbox.client.ui;

import static org.lwjgl.glfw.GLFW.glfwCreateCursor;

import java.io.IOException;
import java.nio.file.Path;
import ritzow.sandbox.client.util.ClientUtility;

public final class Cursors {
	public static long load(Path image, float ratioX, float ratioY) throws IOException {
		var cursorImage = ClientUtility.loadGlfwImage(image);
		return glfwCreateCursor(cursorImage, (int)(cursorImage.width() * ratioX), (int)(cursorImage.height() * ratioY));
	}
}
