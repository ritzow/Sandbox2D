package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.*;
import ritzow.sandbox.util.Utility;

public record RotationAnimation(GuiElement child, double radiansPerNano) implements GuiElement {
	@Override
	public void render(GuiRenderer renderer, long nanos) {
		renderer.draw(child, 1, 0, 0, 1, 1, (float)(System.nanoTime() * radiansPerNano));
	}

	@Override
	public Shape shape() {
		//account for any orientation of child element (could optimize this further for certain shapes)
		Rectangle shape = child.shape().toRectangle();
		return new Circle(Utility.distance(0, 0, shape.width()/2, shape.height()/2));
	}
}
