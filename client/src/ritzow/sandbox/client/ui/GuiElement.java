package ritzow.sandbox.client.ui;

/** A GuiElement is always rendered centered at coordinates [0, 0] with a shape centered at [0, 0]
 * Shape offsets should be achieved with child GuiElements. **/
public interface GuiElement {
	/** Render without mouse hover **/
	void render(GuiRenderer renderer, long nanos);

	/** Render with mouse hover
	 * @return Whether this element consumed this event or not, ie false if the event should
	 * pass through and be notified of mouseover **/
	default boolean render(GuiRenderer renderer, long nanos, float mouseX, float mouseY) {
		render(renderer, nanos);
		return false;
	}

	//TODO implement other mouse events (left, right, middle click, scrolling)
	//TODO add a gui element EmbeddedRenderer that does a flush and switch to a potentially different shader program and parameters, then switches back after.

	/** The shape and size of this GUI element, composite elements should
	 * compute this using the size of their children. If the GuiElement requires
	 * parent bounds to compute its own, this should return some default or minimum shape **/
	Shape shape(); //TODO use shape to cull drawing of GuiElements (ie they are outside of the UI) and possibly for scissoring.

	/** Called when the parent's shape is not bounded by its children **/
	default Shape shape(Rectangle parent) { //TODO this is called even when the parent is dynamically sized
		return shape();
	}

	//default boolean isSizedByChildren()

//	record Scissor(float width, float height) {}
//	default Scissor scissor() {
//		return null;
//	}
}
