package ritzow.sandbox.client.ui.element;

import java.util.PrimitiveIterator.OfInt;
import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.ui.*;

public class Text implements GuiElement {
	private static final float SIZE_SCALE = 0.02f;

	private final CharSequence text;
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
	public Text(CharSequence text, Font font, int size, float spacing) {
		this.text = text;
		this.font = font;
		this.size = size * SIZE_SCALE;
		this.startSpacing = this.size * (1 + spacing);
		long length = text.codePoints().count();
		this.startPos = computeStart(length);
		//TODO remove left vs centered (moved to stuff like absolueguipositioner)
		this.bounds = new Rectangle(length * this.size + Math.max(0, (length - 1) * (this.startSpacing - this.size)), this.size);
	}

	private float computeStart(long length) {
		float gap = this.startSpacing - this.size;
		int half = (int)(length/2);
		if(length % 2 == 0) { //a   b   c | d   e   f
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
		OfInt iterator = text.codePoints().iterator();
		while(iterator.hasNext()) {
			Model model = font.getModel(iterator.nextInt());
			if(model == null) {
				renderer.draw(GameModels.MODEL_RED_SQUARE, 1.0f, pos, 0, size/2, size/2, 0);
			} else {
				renderer.draw(model, 1.0f, pos, 0, size, size, 0);
			}
			pos += startSpacing;
		}
	}

	@Override
	public Shape shape() {
		return bounds;
	}
}
