package ritzow.sandbox.client.ui;

import ritzow.sandbox.client.graphics.ModelRenderer;

//TODO update Text after creating new UI framework
public class Text {
	protected String text;
	protected Font font;
	protected int size;
	protected float spacing;

	public Text(String text, Font font, int size, float spacing) {
		this.text = text;
		this.font = font;
		this.size = size;
		this.spacing = spacing;
	}

	public void render(ModelRenderer renderer) {
		int index = 0;
		float charWidth = (size * 0.02f) + (size * 0.02f * spacing);
		for(float pos = getPositionX(); index < text.length(); pos += charWidth) {
			renderer.queueRender(font.getModel(text.charAt(index)), 1.0f, pos, getPositionY(), size * 0.02f, size * 0.02f, 0.0f);
			index++;
		}
	}

	public float getPositionY() {
		return 0;
	}

	public float getPositionX() {
		return 0;
	}

	public final String getText() {
		return text;
	}

	public final int getSize() {
		return size;
	}

	public final float getSpacing() {
		return spacing;
	}

	public final void setText(String text) {
		this.text = text;
	}

	public final void setSize(int size) {
		this.size = size;
	}

	public final void setSpacing(float spacing) {
		this.spacing = spacing;
	}

	public float getWidth() {
		return 1;
	}

	public float getHeight() {
		return size * 0.02f;
	}
}
