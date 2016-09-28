package graphics;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
	
	public static final int ATTRIBUTE_POSITIONS = 0;
	public static final int ATTRIBUTE_TEXTURE_COORDS = 1;
	
	protected int programID;
	protected Shader[] shaders;
	
	public ShaderProgram(Shader... shaders) {
		this.shaders = shaders;
		programID = glCreateProgram();
		
		for(Shader s : shaders) {
			glAttachShader(programID, s.getShaderID());
		}
		
		glBindAttribLocation(programID, ATTRIBUTE_POSITIONS, "position");
		glBindAttribLocation(programID, ATTRIBUTE_TEXTURE_COORDS, "textureCoord");
		
		glLinkProgram(programID);
		glValidateProgram(programID);
		glUseProgram(programID);
	}
	
	public int getUniformID(String name) {
		return glGetUniformLocation(programID, name);
	}
	
	public void loadMatrix(int uniformID, float[] values) {
		glUniformMatrix4fv(uniformID, true, values);
	}
	
	public void loadInt(int uniformID, int value) {
		glUniform1i(uniformID, value);
	}
	
	public void loadFloat(int uniformID, float value) {
		glUniform1f(uniformID, value);
	}
	
	public void loadVector(int uniformID, float x, float y, float z, float w) {
		glUniform4f(uniformID, x, y, z, w);
	}
	
	public void loadBoolean(int uniformID, boolean value) {
		glUniform1i(uniformID, value ? 1 : 0);
	}
	
	public void use() {
		glUseProgram(programID);
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
