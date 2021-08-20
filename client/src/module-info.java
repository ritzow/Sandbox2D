/**
 * Contains all Sandbox2D client code, but does not export anything. The client can be
 * run by using main class ritzow.sandbox.client.StartClient. TWL's PNGDecoder library
 * is required as an automatic module "PNGDecoder". LWJGL modules
 * org.lwjgl.opengl/openal/glfw are also required.
 * @author Solomon Ritzow
 */
module ritzow.sandbox.client {
	requires ritzow.sandbox.shared;
	requires static java.desktop;
	requires java.logging;
	requires org.lwjgl;
	requires org.lwjgl.glfw;
	requires org.lwjgl.openal;
	requires org.lwjgl.opengl;
}