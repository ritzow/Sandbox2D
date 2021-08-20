package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL46C.*;
/**
 * Represents an OpenGL texture
 * UNUSED TODO delete!
 */
public final class OpenGLByteTexture extends OpenGLTexture {
	public OpenGLByteTexture(ByteBuffer data, int width, int height) {
		super(glGenTextures());
		setup(id);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_R8UI, width, height, 0, GL_RED_INTEGER, GL_UNSIGNED_BYTE, data);
		GraphicsUtility.checkErrors();
	}

	public OpenGLByteTexture(int width, int height) {
		super(glGenTextures());
		setup(id);
		setSize(width, height);
	}

	@Override
	public void setSize(int width, int height) {
		glBindTexture(GL_TEXTURE_2D, id);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_R8UI, width, height, 0, GL_RED_INTEGER, GL_UNSIGNED_BYTE, 0);
	}

	public void setPixel(int x, int y, byte value) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			ByteBuffer val = stack.bytes(value);
			glTextureSubImage2D(id, 0, x, y, 1, 1, GL_RED_INTEGER, GL_UNSIGNED_BYTE, val);
		}
	}
}
