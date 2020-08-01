package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.ui.Font;
import ritzow.sandbox.client.ui.GuiRenderer;

public class TextField extends EditableText {
	private int cursorIndex;

	public TextField(Font font, float size, float spacing) {
		super(font, size, spacing);
		cursorIndex = -1;
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		super.render(renderer, nanos);
		if(renderer.controls().isNewlyPressed(Control.UI_ACTIVATE) && renderer.mousePos().isPresent()) {
			//compute cursorIndex from mouse position and width/height
		}
	}
}
