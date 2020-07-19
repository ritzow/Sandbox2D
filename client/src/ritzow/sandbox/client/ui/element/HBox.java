package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Rectangle;
import ritzow.sandbox.client.ui.Shape;

public class HBox implements GuiElement {
	private final Alignment[] elements;
	private final Rectangle bounds;

	private static record Alignment(GuiElement e, float x) {}

	public HBox(float gap, GuiElement... elements) {
		this.elements = new Alignment[elements.length];
		float width = (elements.length - 1) * gap, maxHeight = 0;
		for(GuiElement e : elements) {
			Rectangle rect = e.shape().toRectangle();
			width += rect.width();
			if(rect.height() > maxHeight) {
				maxHeight = rect.height();
			}
		}

		float posX = -width/2;
		for(int i = 0; i < elements.length; i++) {
			GuiElement e = elements[i];
			Rectangle bounds = e.shape().toRectangle();
			posX += bounds.width()/2;
			this.elements[i] = new Alignment(e, posX);
			posX += bounds.width()/2 + gap;
		}

		this.bounds = new Rectangle(width, maxHeight);
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		for(Alignment e : elements) {
			renderer.draw(e.e, e.x, 0);
		}
	}

	@Override
	public Shape shape() {
		return bounds;
	}
}
