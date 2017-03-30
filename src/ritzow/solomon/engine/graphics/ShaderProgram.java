package ritzow.solomon.engine.graphics;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
	protected final int programID;
	protected final Shader[] shaders;
	
	public ShaderProgram(Shader... shaders) {
		this.shaders = shaders;
		programID = glCreateProgram();
		
		for(Shader s : shaders) {
			glAttachShader(programID, s.shaderID);
			GraphicsUtility.checkErrors();
		}
		
		glLinkProgram(programID);
		glValidateProgram(programID);
		
		GraphicsUtility.checkErrors();
	}
	
	public void setCurrent() {
		glUseProgram(programID);
	}
	
	public int getUniformID(String name) {
		return glGetUniformLocation(programID, name);
	}
	
	public void setMatrix(int uniformID, float[] values) {
		glUniformMatrix4fv(uniformID, true, values);
	}
	
	public void setInteger(int uniformID, int value) {
		glUniform1i(uniformID, value);
	}
	
	public void setFloat(int uniformID, float value) {
		glUniform1f(uniformID, value);
	}
	
	public void setVector(int uniformID, float x, float y, float z) {
		glUniform3f(uniformID, x, y, z);
	}
	
	public void setVector(int uniformID, float x, float y, float z, float w) {
		glUniform4f(uniformID, x, y, z, w);
	}
	
	public void setBoolean(int uniformID, boolean value) {
		glUniform1i(uniformID, value ? 1 : 0);
	}
	
	public int getInteger(int uniformID) {
		return glGetUniformi(programID, uniformID);
	}
	
	public float getFloat(int uniformID) {
		return glGetUniformf(programID, uniformID);
	}
	
	public boolean getBoolean(int uniformID) {
		return glGetUniformi(programID, uniformID) == 1;
	}
	
	public void delete() {
		deleteShaders();
		deleteProgram();
	}
	
	public void deleteProgram() {
		glDeleteProgram(programID);
	}
	
	public void deleteShaders() {
		for(Shader s : shaders) {
			glDetachShader(programID, s.getShaderID());
			s.delete();
		}
	}
}
