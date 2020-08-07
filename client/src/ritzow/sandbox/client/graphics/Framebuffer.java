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

	public int id() {
		return framebufferID;
	}

	public void setCurrent() {
		glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
	}

	public void copyTo(Framebuffer destination, int x, int y, int width, int height) {
		setRead();
		destination.setDraw();
		glBlitFramebuffer(x, y, width, height, x, y, width, height, GL_COLOR_BUFFER_BIT, GL_LINEAR);
	}

	public void clear(float red, float green, float blue, float alpha) {
		setDraw();
		//glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
		glClearColor(red, green, blue, alpha);
		glClear(GL_COLOR_BUFFER_BIT);
	}

	public void attachTextureColor(int texture) {
		attachTexture(texture, GL_COLOR_ATTACHMENT0);
	}

	public void attachTexture(OpenGLTexture texture, int attachment) {
		attachTexture(texture.id, attachment);
	}

	public void attachTexture(int texture, int attachment) {
		glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + attachment, GL_TEXTURE_2D, texture, 0);
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
