package ritzow.sandbox.client.ui;

public final record Rectangle(float width, float height) implements Shape {

	@Override
	public Rectangle toRectangle() {
		return this;
	}

	@Override
	public Shape scale(float scale) {
		return new Rectangle(width * scale, height * scale);
	}
}