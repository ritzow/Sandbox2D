package ritzow.solomon.engine.graphics;

import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glShaderSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

final class Shader {
	protected final int shaderID;

	public Shader(File file, int glShaderType) throws IOException {
		byte[] data = new byte[(int)file.length()];
		FileInputStream reader = new FileInputStream(file);
		reader.read(data);
		reader.close();
		shaderID = glCreateShader(glShaderType);
		glShaderSource(shaderID, new String(data));
		glCompileShader(shaderID);
	}
	
	public int getShaderID() {
		return shaderID;
	}
	
	public void delete() {
		glDeleteShader(shaderID);
	}
}
