package ritzow.sandbox.client.ui;

import java.time.Duration;
import java.util.Optional;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.input.ControlsQuery;

/**
 * The GuiRenderer interface renders GUI elements using the standard coordinate space of [-1, 1] TODO this isn't correct, need to involve framebuffer pixel sizes
 * in the X and Y dimensions to indicate positions within the game window. Nested draws will
 * be affected by parent element transformations and opacities.
 */
public interface GuiRenderer {
	/** Draws a GUI element relative to its parent **/
	void draw(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation);

	/** Draws a GUI element relative to its parent with no relative opacity, scaling, or rotation **/
	void draw(GuiElement element, float posX, float posY);

	/** Draws a GUI element relative to its parent **/
	void draw(GuiElement element, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation);

	void play(Animation animation, Duration duration);

	Rectangle parent();
	ControlsQuery controls();
	Optional<Position> mousePos();
}
