package ritzow.solomon.engine.network;

public class InvalidMessageException extends Exception {
	private static final long serialVersionUID = 1693805154920151682L;

	public InvalidMessageException() {
		super();
	}

	public InvalidMessageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public InvalidMessageException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidMessageException(String message) {
		super(message);
	}

	public InvalidMessageException(Throwable cause) {
		super(cause);
	}
	
	

}
