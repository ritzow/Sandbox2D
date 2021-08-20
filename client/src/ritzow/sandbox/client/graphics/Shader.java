package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL46C.*;

public class Shader {
	private final int shaderID;

	/** Type of OpenGL shader **/
	public enum ShaderType {
		VERTEX(GL_VERTEX_SHADER),
		FRAGMENT(GL_FRAGMENT_SHADER),
		GEOMETRY(GL_GEOMETRY_SHADER),
		COMPUTE(GL_COMPUTE_SHADER);

		private final int glType;

		ShaderType(int glType) {
			this.glType = glType;
		}
	}

	public static Shader fromSource(String programSource, ShaderType type) {
		int shaderID = glCreateShader(type.glType);
		glShaderSource(shaderID, programSource);
		glCompileShader(shaderID);
		Shader shader = new Shader(shaderID);
		GraphicsUtility.checkShaderCompilation(shader);
		return shader;
	}

	public static Shader fromSPIRV(ByteBuffer moduleBinary, ShaderType type) {
		int shaderID = glCreateShader(type.glType);
		try(MemoryStack stack = MemoryStack.stackPush()) {
			glShaderBinary(stack.ints(shaderID), GL_SHADER_BINARY_FORMAT_SPIR_V, moduleBinary);
			IntBuffer empty = stack.mallocInt(0);
			glSpecializeShader(shaderID, "main", empty, empty);
		}
		Shader shader = new Shader(shaderID);
		GraphicsUtility.checkShaderCompilation(shader);
		return shader;
	}

	private Shader(int shaderID) {
		this.shaderID = shaderID;
	}

	public int getShaderID() {
		return shaderID;
	}

	public void delete() {
		glDeleteShader(shaderID);
	}
}
