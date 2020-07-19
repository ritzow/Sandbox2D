package ritzow.sandbox.client.ui;

public final record Rectangle(float width, float height) implements Shape {

	@Override
	public Rectangle toRectangle() {
		return this;
	}
}