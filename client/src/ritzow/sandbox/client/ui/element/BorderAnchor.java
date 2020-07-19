package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.*;

public class BorderAnchor implements GuiElement {


	public BorderAnchor() {
		//TODO might be able to store positions as normalized in -1 to 1 space, then multiply by renderer.parent() bounds to get proper positions
		//however, this might not anchor them to the sides and instead scale them with them, not sure.
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {

	}

	@Override
	public Shape shape() {
		return new Position(0, 0); //TODO implement minimum size so elements don't overlap or which is pleasing to the eye
	}

	@Override
	public Shape shape(Rectangle parent) {
		return parent;
	}
}
