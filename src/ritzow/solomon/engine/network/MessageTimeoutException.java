package ritzow.solomon.engine.network;

public class MessageTimeoutException extends Exception {

	private static final long serialVersionUID = 1L;

	public MessageTimeoutException() {
		super();
	}

	public MessageTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public MessageTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	public MessageTimeoutException(String message) {
		super(message);
	}

	public MessageTimeoutException(Throwable cause) {
		super(cause);
	}
}
