package ritzow.sandbox.data;

public class SerializationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SerializationException() {
		super();
	}

	public SerializationException(String message, Throwable cause, boolean enableSuppression, boolean writeableStackTrace) {
		super(message, cause, enableSuppression, writeableStackTrace);
	}

	public SerializationException(String message, Throwable cause) {
		super(message, cause);
	}

	public SerializationException(String message) {
		super(message);
	}

	public SerializationException(Throwable cause) {
		super(cause);
	}
}
