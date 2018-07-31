package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL20C.glCompileShader;
import static org.lwjgl.opengl.GL20C.glCreateShader;
import static org.lwjgl.opengl.GL20C.glDeleteShader;
import static org.lwjgl.opengl.GL20C.glShaderSource;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

public final class Shader {
	protected final int shaderID;

	public static enum ShaderType {
		VERTEX(GL20.GL_VERTEX_SHADER),
		FRAGMENT(GL20.GL_FRAGMENT_SHADER),
		GEOMETRY(GL32.GL_GEOMETRY_SHADER);

		private int glType;

		ShaderType(int glType) {
			this.glType = glType;
		}
	}

	public static Shader fromSource(String programSource, ShaderType type) {
		return new Shader(programSource, type);
	}

	public static Shader fromSPIRV(ByteBuffer program, ShaderType type) {
		throw new UnsupportedOperationException("not implemented");
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
