package ritzow.sandbox.client.ui;

import ritzow.sandbox.client.graphics.Graphics;

public interface UIElement extends Graphics {
	/**
	 * @param relativeCursorX The x-coordinate of the cursor on the element from -1.0 to 1.0
	 * @param relativeCursorY The y-coordinate of the cursor on the element from -1.0 to 1.0
	 */
	default void onMouseOver(float relativeCursorX, float relativeCursorY) {}
	
	float getWidth();
	float getHeight();
}
