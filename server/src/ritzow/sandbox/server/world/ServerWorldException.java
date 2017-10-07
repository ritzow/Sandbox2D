package ritzow.sandbox.server.world;

public class ServerWorldException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ServerWorldException() {
		super();
	}

	public ServerWorldException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ServerWorldException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServerWorldException(String message) {
		super(message);
	}

	public ServerWorldException(Throwable cause) {
		super(cause);
	}
}
