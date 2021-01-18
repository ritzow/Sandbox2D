package ritzow.sandbox.client.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Similar to Android's SparseIntArray **/
public class ControlsContext implements ControlsQuery {
	//TODO allow only one control toggle per frame, queue last event that happens within frame for next frame
	public void framebufferSize(int width, int height) {}
	public void mouseScroll(double horizontal, double vertical) {}
	public void windowClose() {}
	public void windowFocus(boolean focused) {}
	public void windowIconify(boolean iconified) {}
	public void windowRefresh() {}
	public void onControlPressed(Button button) {}
	public void onControlReleased(Button button) {}
	public void onControlRepeated(Button button) {}

	public void onCharacterTyped(int codePoint) {
		characters.add(codePoint);
	}

	private static final byte
		PREVIOUSLY_RELEASED = 0b00,
		NEWLY_RELEASED = 0b10,
		PREVIOUSLY_PRESSED = 0b01,
		NEWLY_PRESSED = 0b11;

	private final ButtonState[] store;
	private final List<Integer> characters;

	private static class ButtonState {
		private final Button control;
		private byte state;

		private ButtonState(Button control, byte state) {
			this.control = control;
			this.state = state;
		}
	}

	public ControlsContext(Button... controls) {
		store = new ButtonState[controls.length];
		characters = new ArrayList<>();
		Arrays.sort(controls, ControlsContext::compareButtons);
		for(int i = 0; i < controls.length; i++) {
			store[i] = new ButtonState(controls[i], PREVIOUSLY_RELEASED); //TODO test functionality when switching ControlsContexts ie going from main menu to in game
		}
	}

	private static int compareButtons(Button a, Button b) {
		if(a.type() == b.type()) {
			return a.code() - b.code();
		} else {
			return a.type() - b.type();
		}
	}

	private int indexOf(byte type, int code) {
		//binary search copied from OpenJDK Arrays.binarySearch(...)
		int low = 0;
		int high = store.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			ButtonState midVal = store[mid];
			int cmp = midVal.control.type() == type ?
				midVal.control.code() - code : midVal.control.type() - type;

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1);  // key not found.
	}

	public void press(byte type, int key) {
		//do nothing if key isn't in control context
		int index = indexOf(type, key);
		if(index >= 0 && store[index].control.equals(type, key)) {
			store[index].state = NEWLY_PRESSED;
			onControlPressed(store[index].control);
		}
	}

	//TODO if a button is released in the same frame it is pressed, should the release be delayed a frame?
	public void release(byte type, int key) {
		//do nothing if key isn't in control context
		int index = indexOf(type, key);
		if(index >= 0 && store[index].control.equals(type, key)) {
			store[index].state = NEWLY_RELEASED;
			onControlReleased(store[index].control);
		}
	}

	public void repeat(byte type, int key) {
		//do nothing if key isn't in control context
		int index = indexOf(type, key);
		if(index >= 0 && store[index].control.equals(type, key)) {
			onControlRepeated(store[index].control);
		}
	}

	public void nextFrame() {
		for(ButtonState state : store) {
			state.state &= 1; //set the left bit to 0 to indicate it isn't new state
		}
	}

	@Override
	public State state(Button control) {
		return switch(store[indexOf(control.type(), control.code())].state) {
			case PREVIOUSLY_PRESSED -> State.PREVIOUSLY_PRESSED;
			case PREVIOUSLY_RELEASED -> State.PREVIOUSLY_RELEASED;
			case NEWLY_PRESSED -> State.NEWLY_PRESSED;
			case NEWLY_RELEASED -> State.NEWLY_RELEASED;
			default -> throw new IllegalStateException("Unexpected value: " + store[indexOf(control.type(), control.code())].state);
		};
	}

	@Override
	public boolean isPressed(Button control) {
		return (store[indexOf(control.type(), control.code())].state & 1) == 1; //test right bit
	}

	@Override
	public boolean isNewlyPressed(Button control) {
		return store[indexOf(control.type(), control.code())].state == NEWLY_PRESSED;
	}

	@Override
	public boolean isNewlyReleased(Button control) {
		return store[indexOf(control.type(), control.code())].state == NEWLY_RELEASED;
	}
}
