package ritzow.sandbox.protocol;

public final class ConnectionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ConnectionException() {
		
	}
	
	public ConnectionException(String message) {
		super(message);
	}

}
