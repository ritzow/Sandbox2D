package ritzow.sandbox.server;

public class ClientBadDataException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ClientBadDataException() {
	}

	public ClientBadDataException(String arg0) {
		super(arg0);
	}

	public ClientBadDataException(Throwable arg0) {
		super(arg0);
	}

	public ClientBadDataException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public ClientBadDataException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
