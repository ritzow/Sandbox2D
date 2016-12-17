package resource;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glShaderSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

public final class Shaders {
	public static int VERTEX_SHADER;
	public static int FRAGMENT_SHADER;
	
	public static void loadAll(File directory) throws IOException {
		Shaders.VERTEX_SHADER = loadShader(new File(directory, "vertexShader"), GL_VERTEX_SHADER);
		Shaders.FRAGMENT_SHADER = loadShader(new File(directory, "fragmentShader"), GL_FRAGMENT_SHADER);
	}
	
	public static void deleteAll() {
		glDeleteShader(VERTEX_SHADER);
		glDeleteShader(FRAGMENT_SHADER);
	}
	
	public static int loadShader(File file, int OpenGLShaderType) throws IOException {
		FileInputStream reader = new FileInputStream(file);
		
		ByteBuffer data = BufferUtils.createByteBuffer((int)file.length());
		
		while(data.hasRemaining()) {
			data.put((byte)reader.read());
		}
		
		 
		reader.close();
		int shader = glCreateShader(OpenGLShaderType);
		glShaderSource(shader, data.asCharBuffer());
		glCompileShader(shader);
		
		return shader;
	}
}
