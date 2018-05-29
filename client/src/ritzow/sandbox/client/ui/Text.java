package ritzow.sandbox.client.ui;

import ritzow.sandbox.client.graphics.ModelRenderProgram;

//TODO update Text after creating new UI framework
public class Text extends Element {
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
	
	public void render(ModelRenderProgram renderer) { 
		int index = 0;
		float charWidth = (size * 0.02f) + (size * 0.02f * spacing);
		for(float pos = getPositionX(); index < text.length(); pos += charWidth) {
			renderer.render(font.getModelID(text.charAt(index)), 1.0f, pos, getPositionY(), size * 0.02f, size * 0.02f, 0.0f);
			index++;
		}
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
	
	@Override
	public float getWidth() {
		return 1;
	}

	@Override
	public float getHeight() {
		return size * 0.02f;
	}
}
