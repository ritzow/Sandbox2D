package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL20C.glCompileShader;
import static org.lwjgl.opengl.GL20C.glCreateShader;
import static org.lwjgl.opengl.GL20C.glDeleteShader;
import static org.lwjgl.opengl.GL20C.glShaderSource;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

import static org.lwjgl.opengl.GL46C.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.system.MemoryStack;

public class Shader {
	private final int shaderID;

	/** Type of OpenGL shader **/
	public static enum ShaderType {
		VERTEX(GL_VERTEX_SHADER),
		FRAGMENT(GL_FRAGMENT_SHADER),
		GEOMETRY(GL_GEOMETRY_SHADER);

		private final int glType;

		ShaderType(int glType) {
			this.glType = glType;
		}
	}

	public static Shader fromSource(String programSource, ShaderType type) {
		return new Shader(programSource, type);
	}

	public static Shader fromSPIRV(ByteBuffer moduleBinary, ShaderType type) {
		return new Shader(moduleBinary, type);
	}

	private Shader(ByteBuffer buffer, ShaderType type) {
		shaderID = glCreateShader(type.glType);
		try(MemoryStack stack = MemoryStack.stackPush()) {
			glShaderBinary(stack.ints(shaderID), GL_SHADER_BINARY_FORMAT_SPIR_V, buffer);
			IntBuffer empty = stack.mallocInt(0);
			glSpecializeShader(shaderID, "main", empty, empty);
		}
		GraphicsUtility.checkShaderCompilation(this);
	}

	private Shader(String programSource, ShaderType type) {
		shaderID = glCreateShader(type.glType);
		glShaderSource(shaderID, programSource);
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
