package ritzow.solomon.engine.graphics;

import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glShaderSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class Shader {
	protected final int shaderID;

	public Shader(InputStream input, int glShaderType) throws IOException {
		byte[] temp = new byte[input.available()];
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(temp.length);
		
		int read = 0;
		while((read = input.read(temp)) > 0) {
			buffer.write(temp, 0, read);
		}
		
		input.close();
		buffer.flush();
		shaderID = glCreateShader(glShaderType);
		glShaderSource(shaderID, new String(buffer.toByteArray()));
		glCompileShader(shaderID);
	}
	
	public int getShaderID() {
		return shaderID;
	}
	
	public void delete() {
		glDeleteShader(shaderID);
	}
}
