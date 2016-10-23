package graphics.data;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import java.nio.ByteBuffer;

public final class Texture extends Buffer {
	private int textureUnit;
	private ByteBuffer pixels;
	private int width;
	private int height;
	
	public Texture(ByteBuffer pixels, int width, int height) {
		this.pixels = pixels;
		this.width = width;
		this.height = height;
		
		this.objectID = glGenTextures();
		this.textureUnit = 0;
		specifyParameters();
		buffer();
	}
	
	public void specifyParameters() {
		bind();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE); //CLAMP_TO_BORDER causes gaps between models that are adjacent
		unbind();
	}
	
	@Override
	public void buffer() {
		bind();
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
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
}
