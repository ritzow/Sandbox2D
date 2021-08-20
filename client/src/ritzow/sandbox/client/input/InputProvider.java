package ritzow.sandbox.client.input;

/** Queries input **/
public interface InputProvider {
	boolean isControlActivated(Button control);
	int getCursorX();
	int getCursorY();
}
