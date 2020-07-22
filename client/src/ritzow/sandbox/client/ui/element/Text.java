package ritzow.sandbox.client.ui.element;

import java.util.Objects;
import java.util.PrimitiveIterator;
import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.ui.*;

public class Text implements GuiElement {
	private static final float SIZE_SCALE = 0.002f; //0.02f

	private final CharPos[] text;
	private final float scale;
	private final Rectangle bounds;

	private static record CharPos(Model model, float x) {}

	private static final Model MISSING_CHAR_MODEL = GameModels.MODEL_RED_SQUARE;

	/**
	 *
	 * @param text The text to display
	 * @param font The character models usable with the provided GuiRenderer to display
	 * @param size Font size, in points
	 * @param spacing The distance between characters //The fraction of a character width to put between each character
	 * TODO add left justify/centering to constructor and new lines
	 */
	public Text(CharSequence text, Font font, int size, float spacing) {
		this.scale = size * SIZE_SCALE;
		PrimitiveIterator.OfInt scan = text.codePoints().iterator();

		//Compute totals
		int length = 0;
		float glyphWidths = 0;
		float maxHeight = 0;
		while(scan.hasNext()) {
			Model model = Objects.requireNonNullElse(font.getModel(scan.nextInt()), MISSING_CHAR_MODEL);
			glyphWidths += model.width();
			if(model.height() > maxHeight) maxHeight = model.height();
			length++;
		}
		float totalWidth = glyphWidths * scale + Math.max(length - 1, 0) * spacing;
		this.bounds = new Rectangle(totalWidth, maxHeight * scale);
		this.text = new CharPos[(int)length];

		//TODO fix positioning of error model
		//Compute positions
		int index = 0;
		float pos = -totalWidth/2f;
		PrimitiveIterator.OfInt iterator = text.codePoints().iterator();
		while(iterator.hasNext()) {
			Model model = Objects.requireNonNullElse(font.getModel(iterator.nextInt()), MISSING_CHAR_MODEL);
			pos += model.width()/2f * scale;
			this.text[index] = new CharPos(model, pos);
			pos += model.width()/2f * scale + spacing;
			index++;
		}
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		for(CharPos data : text) {
			renderer.draw(data.model, 1.0f, data.x, 0, scale, scale, 0);
		}
	}

	@Override
	public Shape shape() {
		return bounds;
	}

	@Override
	public Shape shape(Rectangle parent) {
		return bounds;
	}
}
