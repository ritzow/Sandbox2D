package ritzow.sandbox.data;

public class ClassNotRegisteredException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ClassNotRegisteredException() {
	}

	public ClassNotRegisteredException(String message) {
		super(message);
	}

	public ClassNotRegisteredException(Throwable cause) {
		super(cause);
	}

	public ClassNotRegisteredException(String message, Throwable cause) {
		super(message, cause);
	}

	public ClassNotRegisteredException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
