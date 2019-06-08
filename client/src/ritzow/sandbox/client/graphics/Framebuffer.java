package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL30C.*;

public final class Framebuffer {
	public final int framebufferID;
	
	public Framebuffer(int id) {
		this.framebufferID = id;
	}

	public Framebuffer() {
		this.framebufferID = glGenFramebuffers();

		GraphicsUtility.checkErrors();
	}

	public void copyTo(Framebuffer destination, int x, int y, int width, int height) {
		setRead();
		destination.setDraw();
		glBlitFramebuffer(x, y, width, height, x, y, width, height, GL_COLOR_BUFFER_BIT, GL_LINEAR);
	}

	public void clear(float red, float green, float blue, float alpha) {
		setDraw();
		glClearColor(red, green, blue, alpha);
		glClear(GL_COLOR_BUFFER_BIT);
	}

	public void attachTexture(int texture, int location) {
		glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
		glBindTexture(GL_TEXTURE_2D, texture);
		glFramebufferTexture2D(GL_FRAMEBUFFER, location, GL_TEXTURE_2D, texture, 0);
	}

	public void attachTexture(OpenGLTexture texture, int location) {
		glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
		texture.bind();
		glFramebufferTexture2D(GL_FRAMEBUFFER, location, GL_TEXTURE_2D, texture.id, 0);
	}

	public void setRead() {
		glBindFramebuffer(GL_READ_FRAMEBUFFER, framebufferID);
	}

	public void setDraw() {
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebufferID);
	}

	public void delete() {
		glDeleteFramebuffers(framebufferID);
	}
}
