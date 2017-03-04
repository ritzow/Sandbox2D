package ritzow.solomon.engine.graphics;

import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glBindAttribLocation;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDetachShader;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glValidateProgram;

class ShaderProgram {
	public static final int ATTRIBUTE_POSITIONS = 0;
	public static final int ATTRIBUTE_TEXTURE_COORDS = 1;
	public static final int ATTRIBUTE_INDICES = 2;
	
	protected final int programID;
	protected final Shader[] shaders;
	
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
