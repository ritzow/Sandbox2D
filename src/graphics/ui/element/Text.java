package graphics.ui.element;

import graphics.Renderer;
import util.ModelManager;

public class Text extends Element {
	
	protected String text;
	protected int size;
	
	public Text(String text, int size) {
		this.text = text;
		this.size = size;
	}

	@Override
	public void render(Renderer renderer, float x, float y) {
		for(int i = 0; i < text.length(); i++) {
			renderer.loadTransformationMatrix(x + i/20.0f, y, size/20.0f, size/20.0f, 0);
			ModelManager.lookupCharacter(text.charAt(i)).render();
		}
	}

	public final String getText() {
		return text;
	}

	public final void setText(String text) {
		this.text = text;
	}

	public final int getSize() {
		return size;
	}

	public final void setSize(int size) {
		this.size = size;
	}

}
