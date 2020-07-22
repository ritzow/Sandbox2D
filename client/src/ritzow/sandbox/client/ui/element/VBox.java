package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Rectangle;
import ritzow.sandbox.client.ui.Shape;

//TODO VBox and HBox should check if children have changed scale and recompute bounds
public class VBox implements GuiElement {
	private final Alignment[] elements;
	private final Rectangle bounds;

	private static record Alignment(GuiElement e, float y) {}

	public VBox(float gap, GuiElement... elements) {
		this.elements = new Alignment[elements.length];
		float height = (elements.length - 1) * gap, maxWidth = 0;
		for(GuiElement e : elements) {
			Rectangle rect = e.shape().toRectangle();
			height += rect.height();
			if(rect.width() > maxWidth) {
				maxWidth = rect.width();
			}
		}

		float posY = height/2;
		for(int i = 0; i < elements.length; i++) {
			GuiElement e = elements[i];
			Rectangle bounds = e.shape().toRectangle();
			posY -= bounds.height()/2;
			this.elements[i] = new Alignment(e, posY);
			posY -= bounds.height()/2 + gap;
		}

		this.bounds = new Rectangle(maxWidth, height);
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		for(Alignment e : elements) {
			renderer.draw(e.e, 0, e.y());
		}
	}

	@Override
	public Shape shape() {
		return bounds;
	}
}
