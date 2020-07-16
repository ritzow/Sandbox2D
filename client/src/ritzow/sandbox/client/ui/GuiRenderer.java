package ritzow.sandbox.client.ui;

import ritzow.sandbox.client.graphics.Graphics;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.ui.element.GuiElement;

public interface GuiRenderer {
	/** Draws a GUI element relative to its parent **/
	void draw(Graphics graphics, float posX, float posY);

	/** Draws a GUI element relative to its parent **/
	void draw(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation);

	/** Draws a GUI element relative to its parent **/
	void draw(GuiElement element, float posX, float posY);
}
