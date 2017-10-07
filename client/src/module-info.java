/**
 * @author Solomon Ritzow
 *
 */
module ritzow.sandbox.client {
	requires transitive ritzow.sandbox.shared; //share with anything using Sandbox2DClient as a library
	requires PNGDecoder;
	requires org.lwjgl;
	requires org.lwjgl.opengl;
	requires org.lwjgl.openal;
	requires org.lwjgl.glfw;
}