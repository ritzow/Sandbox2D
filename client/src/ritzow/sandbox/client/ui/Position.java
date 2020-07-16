package ritzow.sandbox.client.ui;

public final record Position(float x, float y) {

	public static Position of(float x, float y) {
		return new Position(x, y);
	}
}
