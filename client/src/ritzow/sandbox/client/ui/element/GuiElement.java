package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.GuiRenderer;

public interface GuiElement {
	void render(GuiRenderer renderer);
	//void onHover(float localX, float localY);
	default void update(long nanoseconds) {}
	//void setOnClick(Runnable action);
	//float width();
	//float height();
}
