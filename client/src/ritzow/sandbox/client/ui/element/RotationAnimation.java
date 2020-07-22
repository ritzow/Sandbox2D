package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.*;
import ritzow.sandbox.util.Utility;

public class RotationAnimation implements GuiElement {
	private final GuiElement child;
	private final double radiansPerNano;
	private float rotation;

	public RotationAnimation(GuiElement child, double radiansPerNano) {
		this.child = child;
		this.radiansPerNano = radiansPerNano;
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		renderer.draw(child, 1, 0, 0, 1, 1, rotation);
		rotation += nanos * radiansPerNano;
	}

	@Override
	public Shape shape() {
		//account for any orientation of child element (could optimize this further for certain shapes)
		Rectangle shape = child.shape().toRectangle();
		return new Circle(Utility.distance(0, 0, shape.width()/2, shape.height()/2));
	}
}
