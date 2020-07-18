package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.*;

public class Text implements GuiElement {
	private static final float SIZE_SCALE = 0.02f;

	private final String text;
	private final Font font;
	private final float startPos, spacing, size;

	public enum Layout {
		LEFT,
		CENTERED
	}

	/**
	 *
	 * @param text The text to display
	 * @param font The character models usable with the provided GuiRenderer to display
	 * @param layout How the text should be offset from the origin
	 * @param size Font size, in points
	 * @param spacing The fraction of a character width to put between each character
	 */
	public Text(String text, Font font, Layout layout, int size, float spacing) {
		this.text = text;
		this.font = font;
		this.size = size * SIZE_SCALE;
		this.spacing = this.size * (1 + spacing);
		this.startPos = switch(layout) {
			case LEFT -> 0;
			case CENTERED -> {
				float gap = this.spacing - this.size;
				if(text.length() % 2 == 0) { //a   b   c | d   e   f
					yield -gap/2f - (text.length()/2 * this.size) - (text.length()/2 - 1) * gap + this.size/2;
				} else { //a b c|c d e
					yield -(this.size + gap) * (text.length()/2);
				}
			}
		};
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		//TODO use text.codePoints().iterator() instead to more properly parse
		//TODO include constructor field for whether to center text or start at left
		float pos = startPos;
		for(int index = 0; index < text.length(); pos += spacing, index++) {
			renderer.draw(font.getModel(text.charAt(index)), 1.0f, pos, 0, size, size, 0);
		}
	}

	@Override
	public Shape shape() {
		return new Rectangle(Math.abs(startPos) * 2, size);
	}
}
