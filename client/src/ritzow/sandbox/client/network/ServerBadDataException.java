package ritzow.sandbox.client.network;

public class ServerBadDataException extends RuntimeException {
	public ServerBadDataException() {
		super();
	}

	public ServerBadDataException(String message) {
		super(message);
	}

	public ServerBadDataException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServerBadDataException(Throwable cause) {
		super(cause);
	}

	protected ServerBadDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
