package ritzow.sandbox.client.ui;

import ritzow.sandbox.util.Utility;

public record Circle(float radius) implements Shape {
	@Override
	public Rectangle toRectangle() {
		return new Rectangle(radius * 2, radius * 2);
	}

	@Override
	public Shape scale(float scale) {
		return new Circle(radius * scale);
	}

	@Override
	public boolean intersects(Position point) {
		return Utility.withinDistance(point.x(), point.y(), 0, 0, radius);
	}
}
