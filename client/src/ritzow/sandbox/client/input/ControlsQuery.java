package ritzow.sandbox.client.input;

public interface ControlsQuery {
	boolean isPressed(Button control);
	boolean isNewlyPressed(Button control);
	boolean isNewlyReleased(Button control);
	State state(Button control);
	//TODO could potentially make state modification a thing, so something can disable/"consume" a control, like a UI element
	//TODO implement mouse scroll polling in ControlsQuery

	enum State {
		PREVIOUSLY_PRESSED,
		PREVIOUSLY_RELEASED,
		NEWLY_PRESSED,
		NEWLY_RELEASED
	}
}
