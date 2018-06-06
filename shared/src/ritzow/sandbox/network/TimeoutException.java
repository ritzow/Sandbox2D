package ritzow.sandbox.network;

public class TimeoutException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public TimeoutException() {
		super(null, null, false, false);
	}

	public TimeoutException(String message, Throwable cause) {
		super(message, cause, false, false);
	}

	public TimeoutException(String message) {
		super(message, null, false, false);
	}

	public TimeoutException(Throwable cause) {
		super(null, cause, false, false);
	}
}
