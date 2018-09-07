package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL46C.*;

public class ShaderProgram {
	protected final int programID;
	protected final Shader[] shaders; //stores the shaders for deletion

	public ShaderProgram(Shader... shaders) {
		this.shaders = shaders;
		programID = glCreateProgram();

		for(Shader s : shaders) {
			glAttachShader(programID, s.getShaderID());
			GraphicsUtility.checkErrors();
		}

		glLinkProgram(programID);
		glValidateProgram(programID);

		GraphicsUtility.checkProgramCompilation(this);
	}

	@Deprecated
	public void setCurrent() {
		glUseProgram(programID);
	}

	protected final int getUniformLocation(String name) {
		return glGetUniformLocation(programID, name);
	}

	/**
	 * Associate a texture unit with a sampler2D object in a fragment shader
	 * @param samplerUniformID the texture sampler
	 * @param samplerIndex the texture unit to associate the sampler with
	 */
	public final void setSamplerIndex(int samplerUniformID, int textureUnit) {
		setInteger(samplerUniformID, textureUnit);
	}

	/**
	 * Put a texture in a texture unit
	 * @param textureUnit the texture unit
	 * @param textureID the texture
	 */
	@SuppressWarnings("static-method")
	public final void setTexture(int textureUnit, int textureID) {
		glActiveTexture(GL_TEXTURE0 + textureUnit);
		glBindTexture(GL_TEXTURE_2D, textureID);
	}

	public final void setMatrix(int uniformID, float[] values) {
		glProgramUniformMatrix4fv(programID, uniformID, true, values);
	}

	public final void setInteger(int uniformID, int value) {
		glProgramUniform1i(programID, uniformID, value);
	}

	public final void setFloat(int uniformID, float value) {
		glProgramUniform1f(programID, uniformID, value);
	}

	public final void setVector(int uniformID, float x, float y, float z) {
		glProgramUniform3f(programID, uniformID, x, y, z);
	}

	public final void setVector(int uniformID, float x, float y, float z, float w) {
		glProgramUniform4f(programID, uniformID, x, y, z, w);
	}

	public final void setBoolean(int uniformID, boolean value) {
		glProgramUniform1i(programID, uniformID, value ? 1 : 0);
	}

	public final int getInteger(int uniformID) {
		return glGetUniformi(programID, uniformID);
	}

	public final float getFloat(int uniformID) {
		return glGetUniformf(programID, uniformID);
	}

	public final boolean getBoolean(int uniformID) {
		return glGetUniformi(programID, uniformID) == 1;
	}

	public void delete() {
		deleteShaders();
		deleteProgram();
	}

	public final void deleteProgram() {
		glDeleteProgram(programID);
	}

	public void deleteShaders() {
		for(Shader s : shaders) {
			glDetachShader(programID, s.getShaderID());
			s.delete();
		}
	}
}
