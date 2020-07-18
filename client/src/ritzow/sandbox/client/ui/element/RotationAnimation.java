package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.GuiRenderer;

public record RotationAnimation(GuiElement child, double radiansPerNano) implements GuiElement {
	@Override
	public void render(GuiRenderer renderer) {
		renderer.draw(child, 1, 0, 0, 1, 1, (float)(System.nanoTime() * radiansPerNano));
	}
}
