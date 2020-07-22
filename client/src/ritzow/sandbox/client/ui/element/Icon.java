package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.graphics.MutableGraphics;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Rectangle;
import ritzow.sandbox.client.ui.Shape;

public class Icon implements GuiElement {
	private final MutableGraphics graphics;

	public Icon(Model modelID) {
		this.graphics = new MutableGraphics(modelID, 1.0f, 1.0f,  0, 1.0f);
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		renderer.draw(graphics.model, graphics.opacity, 0, 0, graphics.scaleX, graphics.scaleY, graphics.rotation);
	}

	@Override
	public Shape shape() {
		//TODO rotated rectangle?
		return new Rectangle(graphics.model.width() * graphics.scaleX, graphics.model.height() * graphics.scaleY);
	}
}
