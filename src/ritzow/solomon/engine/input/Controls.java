package ritzow.solomon.engine.input;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F11;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;

/**
 * Some constants for GLFW keycodes used in controllers
 * @author Solomon Ritzow
 *
 */
public final class Controls {
	public static int KEYBIND_UP = 				GLFW_KEY_UP; //GLFW_KEY_W;
	public static int KEYBIND_DOWN = 			GLFW_KEY_DOWN; //GLFW_KEY_S;
	public static int KEYBIND_LEFT = 			GLFW_KEY_LEFT; //GLFW_KEY_A;
	public static int KEYBIND_RIGHT = 			GLFW_KEY_RIGHT; //GLFW_KEY_D;
	public static int KEYBIND_INCREASEZOOM = 	GLFW_KEY_KP_ADD;
	public static int KEYBIND_DECREASEZOOM = 	GLFW_KEY_KP_SUBTRACT;
	public static int KEYBIND_QUIT = 			GLFW_KEY_ESCAPE;
	public static int KEYBIND_FULLSCREEN =		GLFW_KEY_F11; //GLFW_KEY_F;
	public static int KEYBIND_ACTIVATE =		GLFW_KEY_E;
}
