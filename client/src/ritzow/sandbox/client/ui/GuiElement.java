package ritzow.sandbox.client.ui;

public interface GuiElement {
	record Scissor(float width, float height) {}

	/** Render without mouse hover **/
	void render(GuiRenderer renderer, long nanos);

	/** Render with mouse hover
	 * @return Whether this element consumed this event or not, ie false if the event should
	 * pass through and be notified of mouseover **/
	default boolean render(GuiRenderer renderer, long nanos, float mouseX, float mouseY) {
		render(renderer, nanos);
		return false;
	}

	/** The shape and size of this GUI element, composite elements should
	 * compute this using the size of their children. **/
	Shape shape();

//	default Scissor scissor() {
//		return null;
//	}
}
