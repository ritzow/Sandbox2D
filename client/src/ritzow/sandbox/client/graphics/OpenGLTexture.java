package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;

/**
 * Represents an OpenGL texture
 * UNUSED TODO delete!
 */
public class OpenGLTexture {
	public final int id;

	public OpenGLTexture(int id) {
		this.id = id;
	}

	public OpenGLTexture(ByteBuffer pixels, int width, int height) {
		this.id = glGenTextures();
		setup(id);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
		glBindTexture(GL_TEXTURE_2D, 0);
		GraphicsUtility.checkErrors();
	}

	public OpenGLTexture(int width, int height) {
		this.id = glGenTextures();
		setup(id);
		setSize(width, height);
	}

	public void setSize(int width, int height) {
		glBindTexture(GL_TEXTURE_2D, id);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
	}

	protected static void setup(int texture) {
		glBindTexture(GL_TEXTURE_2D, texture);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	public void bind() {
		glBindTexture(GL_TEXTURE_2D, id);
	}

	public void delete() {
		glDeleteTextures(id);
	}
}
