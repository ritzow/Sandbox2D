package ritzow.sandbox.server;

public class ClientBadDataException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ClientBadDataException() {
		super();
	}
	
	public ClientBadDataException(String reason) {
		super(reason);
	}
}
