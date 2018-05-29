package ritzow.sandbox.client.audio;

public class OpenALException extends RuntimeException {

	public OpenALException() {
	}

	public OpenALException(String message) {
		super(message);
	}

	public OpenALException(Throwable cause) {
		super(cause);
	}

	public OpenALException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpenALException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
