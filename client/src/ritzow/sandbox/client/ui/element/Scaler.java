package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Shape;

public record Scaler(GuiElement element, float scale) implements GuiElement {
	@Override
	public void render(GuiRenderer renderer, long nanos) {
		renderer.draw(element, 1, 0, 0, scale, scale, 0);
	}

	@Override
	public Shape shape() {
		return element.shape().scale(scale); //TODO multiply by scale?
	}
}
