package ritzow.sandbox.client.ui;

public /*sealed*/ interface Shape /*permits Rectangle, InfinitePlane*/ {
	Rectangle toRectangle();
	Shape scale(float scale);
}
