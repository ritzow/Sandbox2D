package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glShaderSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

public final class Shader {
	protected final int shaderID;
	
	public static enum ShaderType {
		VERTEX(GL20.GL_VERTEX_SHADER),
		FRAGMENT(GL20.GL_FRAGMENT_SHADER),
		GEOMETRY(GL32.GL_GEOMETRY_SHADER);
		
		int glType;
		
		ShaderType(int glType) {
			this.glType = glType;
		}
	}

	public Shader(InputStream input, ShaderType type) throws IOException {
		byte[] temp = new byte[input.available()];
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(temp.length);
		int read = 0;
		while((read = input.read(temp)) > 0) {
			buffer.write(temp, 0, read);
		}
		
		input.close();
		buffer.flush();
		shaderID = glCreateShader(type.glType);
		glShaderSource(shaderID, new String(buffer.toByteArray()));
		glCompileShader(shaderID);
		
		GraphicsUtility.checkShaderCompilation(this);
	}
	
	public int getShaderID() {
		return shaderID;
	}
	
	public void delete() {
		glDeleteShader(shaderID);
	}
}
