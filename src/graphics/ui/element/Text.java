package graphics.ui.element;

import graphics.Renderer;
import resource.Font;

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

	@Override 
	public void render(Renderer renderer, float x, float y) {
		renderer.loadOpacity(1.0f);
		
		int index = 0;
		float charWidth = size * 0.02f + spacing * size * 0.02f;
		for(float pos = x; index < text.length(); pos += charWidth) {
			renderer.loadTransformationMatrix(pos, y, size * 0.02f, size * 0.021f, 0);
			font.getModel(text.charAt(index)).render();
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
	
	public float getWidth() {
		return 0;
		//TODO implement this method
	}

}
