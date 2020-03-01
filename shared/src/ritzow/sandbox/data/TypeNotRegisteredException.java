package ritzow.sandbox.data;

public class TypeNotRegisteredException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TypeNotRegisteredException() {
	}

	public TypeNotRegisteredException(String message) {
		super(message);
	}

	public TypeNotRegisteredException(Throwable cause) {
		super(cause);
	}

	public TypeNotRegisteredException(String message, Throwable cause) {
		super(message, cause);
	}

	public TypeNotRegisteredException(String message, Throwable cause, boolean enableSuppression,
									  boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
