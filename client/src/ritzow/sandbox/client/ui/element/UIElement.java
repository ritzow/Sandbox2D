package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.graphics.Graphics;

public interface UIElement {
	Iterable<Graphics> appearance();
	void onHover(float localX, float localY);
	void onClick();
	float width();
	float height();
}
