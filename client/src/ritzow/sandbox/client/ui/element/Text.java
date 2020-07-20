package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.*;

public class Text implements GuiElement {
	private static final float SIZE_SCALE = 0.02f;

	private final String text;
	private final Font font;
	private final float startPos, startSpacing, size;
	private final Rectangle bounds;

	/**
	 *
	 * @param text The text to display
	 * @param font The character models usable with the provided GuiRenderer to display
	 * @param size Font size, in points
	 * @param spacing The fraction of a character width to put between each character
	 */
	public Text(String text, Font font, int size, float spacing) {
		this.text = text;
		this.font = font;
		this.size = size * SIZE_SCALE;
		this.startSpacing = this.size * (1 + spacing);
		this.startPos = computeStart();
		//TODO remove left vs centered (moved to stuff like absolueguipositioner)
		this.bounds = new Rectangle(text.length() * this.size + Math.max(0, (text.length() - 1) * (this.startSpacing - this.size)), this.size);
	}

	private float computeStart() {
		float gap = this.startSpacing - this.size;
		int half = text.length()/2;
		if(text.length() % 2 == 0) { //a   b   c | d   e   f
			return -gap/2f - (half * this.size) - (half - 1) * gap + this.size/2;
		} else { //a b c|c d e
			return -(this.size + gap) * half;
		}
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		//TODO use text.codePoints().iterator() instead to more properly parse
		//TODO include constructor field for whether to center text or start at left
		float pos = startPos;
		for(int index = 0; index < text.length(); pos += startSpacing, index++) {
			renderer.draw(font.getModel(text.charAt(index)), 1.0f, pos, 0, size, size, 0);
		}
	}

	@Override
	public Shape shape() {
		return bounds;
	}
}
