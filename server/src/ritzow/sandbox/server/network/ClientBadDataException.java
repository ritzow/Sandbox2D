package ritzow.sandbox.server.network;

public class ClientBadDataException extends RuntimeException {

	public ClientBadDataException() {
		super(null, null, false, false);
	}
	
	public ClientBadDataException(String reason) {
		super(reason, null, false, false);
	}
}
