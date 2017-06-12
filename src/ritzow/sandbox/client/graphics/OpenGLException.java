package ritzow.sandbox.client.graphics;

public class OpenGLException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public OpenGLException() {
		super();
	}

	public OpenGLException(String message) {
		super(message);
	}

	public OpenGLException(Throwable cause) {
		super(cause);
	}

	public OpenGLException(String message, Throwable cause) {
		super(message, cause);
	}
}
