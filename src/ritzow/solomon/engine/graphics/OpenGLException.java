package ritzow.solomon.engine.graphics;

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

	public OpenGLException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
	public static void checkErrors() {
		int error = org.lwjgl.opengl.GL11.glGetError();
		if(error != 0) {
			StringBuilder errorMsg = new StringBuilder("Error Codes: ");
			errorMsg.append(error);
			errorMsg.append(", ");
			while((error = org.lwjgl.opengl.GL11.glGetError()) != 0) {
				errorMsg.append(error);
				errorMsg.append(", ");
			}
			throw new OpenGLException(errorMsg.toString());
		}
	}

}
