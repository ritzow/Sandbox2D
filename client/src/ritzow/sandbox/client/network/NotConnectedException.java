package ritzow.sandbox.client.network;

public class NotConnectedException extends RuntimeException {

	public NotConnectedException() {
		super();
	}

	public NotConnectedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NotConnectedException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotConnectedException(String message) {
		super(message);
	}

	public NotConnectedException(Throwable cause) {
		super(cause);
	}
}
