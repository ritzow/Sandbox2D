package ritzow.solomon.engine.graphics;

public class OpenGLException extends Error {

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
