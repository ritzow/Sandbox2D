package graphics.data;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.stb.STBImage.*;
import org.lwjgl.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Texture extends Buffer {
	IntBuffer width			= BufferUtils.createIntBuffer(1);
	IntBuffer height		= BufferUtils.createIntBuffer(1);
	IntBuffer numComponents	= BufferUtils.createIntBuffer(1);
	protected int textureUnit;
	
	protected ByteBuffer pixels;

	public Texture(String file) {
		this.objectID = glGenTextures();
		stbi_set_flip_vertically_on_load(1);
		this.pixels = stbi_load(file, width, height, numComponents, 4);
		this.textureUnit = 0;
		specifyParameters();
		buffer();
		stbi_image_free(pixels);
	}
	
	public void specifyParameters() {
		bind();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		unbind();
	}
	
	@Override
	public void buffer() {
		bind();
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, getWidth(), getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
		unbind();
	}

	@Override
	public void bind() {
		glActiveTexture(GL_TEXTURE0 + textureUnit);
		glBindTexture(GL_TEXTURE_2D, objectID);
	}

	@Override
	public void unbind() {
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, 0);
	}
	
	@Override
	public void delete() {
		glDeleteTextures(objectID);
	}
	
	public int getWidth() {
		return width.get(0);
	}
	
	public int getHeight() {
		return height.get(0);
	}
}
