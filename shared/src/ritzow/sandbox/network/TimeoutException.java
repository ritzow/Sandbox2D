package ritzow.sandbox.network;

public class TimeoutException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private static final boolean STACK_TRACE = true;

	public TimeoutException() {
		super(null, null, false, STACK_TRACE);
	}

	public TimeoutException(String message) {
		super(message, null, false, STACK_TRACE);
	}
}
