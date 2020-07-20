package ritzow.sandbox.client.ui;

public final record Position(float x, float y) implements Shape {

	public static Position of(float x, float y) {
		return new Position(x, y);
	}

	@Override
	public Rectangle toRectangle() {
		return new Rectangle(0, 0);
	}

	@Override
	public Shape scale(float scale) {
		return this;
	}

	public Position translate(float x, float y) {
		return new Position(this.x + x, this.y + y);
	};
}
