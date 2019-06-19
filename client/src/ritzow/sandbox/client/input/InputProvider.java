package ritzow.sandbox.client.input;

import ritzow.sandbox.client.input.Control.Button;

/** Queries input **/
public interface InputProvider {
	boolean isControlActivated(Button control);
	int getCursorX();
	int getCursorY();
}
