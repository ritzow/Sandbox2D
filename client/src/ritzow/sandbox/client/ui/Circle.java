package ritzow.sandbox.client.ui;

public record Circle(float radius) implements Shape {
	@Override
	public Rectangle toRectangle() {
		return new Rectangle(radius * 2, radius * 2);
	}
}
