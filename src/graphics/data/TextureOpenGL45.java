package graphics.data;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.stb.STBImage.*;
import org.lwjgl.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class TextureOpenGL45 extends Buffer {
	IntBuffer width			= BufferUtils.createIntBuffer(1);
	IntBuffer height		= BufferUtils.createIntBuffer(1);
	IntBuffer numComponents	= BufferUtils.createIntBuffer(1);
	protected int textureUnit;
	
	protected ByteBuffer pixels;

	public TextureOpenGL45(String file) {
		this.objectID = glCreateTextures(GL_TEXTURE_2D);
		stbi_set_flip_vertically_on_load(true);
		this.pixels = stbi_load(file, width, height, numComponents, 4);
		this.textureUnit = 0;
		buffer();
	}
	
	public void specifyTextureParameters() {
		glTextureParameteri(objectID, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTextureParameteri(objectID, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTextureParameteri(objectID, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTextureParameteri(objectID, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}
	
	@Override
	public void buffer() {
		bind();
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, this.getWidth(), this.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
	}

	@Override
	public void bind() {
		glBindTextureUnit(textureUnit, objectID);
	}

	@Override
	public void unbind() {
		glBindTextureUnit(0, objectID);
	}
	
	@Override
	public void delete() {
		glDeleteTextures(objectID);
		stbi_image_free(pixels);
	}
	
	public int getWidth() {
		return width.get(0);
	}
	
	public int getHeight() {
		return height.get(0);
	}
}
