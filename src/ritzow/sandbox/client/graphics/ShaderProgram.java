package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.*;

@SuppressWarnings("static-method")
public class ShaderProgram {
	protected final int programID;
	protected final Shader[] shaders; //stores the shaders for deletion
	
	public ShaderProgram(Shader... shaders) {
		this.shaders = shaders;
		programID = glCreateProgram();
		
		for(Shader s : shaders) {
			glAttachShader(programID, s.shaderID);
			GraphicsUtility.checkErrors();
		}
		
		glLinkProgram(programID);
		glValidateProgram(programID);
		
		GraphicsUtility.checkProgramCompilation(this);
	}
	
	public final void setCurrent() {
		glUseProgram(programID);
	}
	
	protected final int getUniformID(String name) {
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
	public final void setTexture(int textureUnit, int textureID) {
		glActiveTexture(GL_TEXTURE0 + textureUnit);
		glBindTexture(GL_TEXTURE_2D, textureID);
	}
	
	public final void setMatrix(int uniformID, float[] values) {
		glUniformMatrix4fv(uniformID, true, values);
	}
	
	public final void setInteger(int uniformID, int value) {
		glUniform1i(uniformID, value);
	}
	
	public final void setFloat(int uniformID, float value) {
		glUniform1f(uniformID, value);
	}
	
	public final void setVector(int uniformID, float x, float y, float z) {
		glUniform3f(uniformID, x, y, z);
	}
	
	public final void setVector(int uniformID, float x, float y, float z, float w) {
		glUniform4f(uniformID, x, y, z, w);
	}
	
	public final void setBoolean(int uniformID, boolean value) {
		glUniform1i(uniformID, value ? 1 : 0);
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
