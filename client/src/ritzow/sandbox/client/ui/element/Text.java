package ritzow.sandbox.client.ui.element;

import java.util.Objects;
import java.util.PrimitiveIterator;
import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.ui.*;
import ritzow.sandbox.client.util.ClientUtility;

public class Text implements GuiElement {
	private static final float SIZE_SCALE = 0.002f;
	private final CharPos[] text;
	private final Rectangle bounds;

	private static record CharPos(Model model, float x, float scale) {}

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
		//compute max height
		float maxHeight = 0;
		{
			PrimitiveIterator.OfInt scan = text.codePoints().iterator();
			while(scan.hasNext()) {
				Model model = font.getModel(scan.nextInt());
				if(model != null && model.height() > maxHeight) maxHeight = model.height();
			}
		}
		float missingScale = ClientUtility.scaleToHeight(maxHeight, MISSING_CHAR_MODEL);

		//Compute total width
		int length = 0;
		float glyphWidths = 0;
		PrimitiveIterator.OfInt scan = text.codePoints().iterator();
		while(scan.hasNext()) {
			Model model = Objects.requireNonNullElse(font.getModel(scan.nextInt()), MISSING_CHAR_MODEL);
			glyphWidths += model.width() * (model == MISSING_CHAR_MODEL ? missingScale : 1);
			length++;
		}
		float scale = size * SIZE_SCALE;
		float totalWidth = glyphWidths * scale + Math.max(length - 1, 0) * spacing;
		this.bounds = new Rectangle(totalWidth, maxHeight * scale);
		this.text = new CharPos[length];

		//Compute positions
		int index = 0;
		float pos = -totalWidth/2f;
		PrimitiveIterator.OfInt iterator = text.codePoints().iterator();
		while(iterator.hasNext()) {
			Model model = Objects.requireNonNullElse(font.getModel(iterator.nextInt()), MISSING_CHAR_MODEL);
			float charScale = (model == MISSING_CHAR_MODEL ? missingScale * scale : scale);
			float width = model.width() * charScale;
			pos += width/2f;
			this.text[index] = new CharPos(model, pos, charScale);
			pos += width/2f + spacing;
			index++;
		}
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		for(CharPos data : text) {
			renderer.draw(data.model, 1.0f, data.x, 0, data.scale, data.scale, 0);
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
