package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;

import java.nio.ByteBuffer;

/**
 * Represents an OpenGL texture
 */
public final class OpenGLTexture {
	public final int id;
	private final boolean fromImage;

	public OpenGLTexture(ByteBuffer pixels, int width, int height) {
		this.id = glGenTextures();
		this.fromImage = true;
		setup(id);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
		glBindTexture(GL_TEXTURE_2D, 0);
		GraphicsUtility.checkErrors();
	}

	public OpenGLTexture(int width, int height) {
		this.id = glGenTextures();
		this.fromImage = false;
		setSize(width, height);
	}

	public void setSize(int width, int height) {
		if(fromImage)
			throw new RuntimeException("cannot resize an image created from a texture");
		setup(id);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
	}

	private static final void setup(int texture) {
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
