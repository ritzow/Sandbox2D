package ritzow.solomon.engine.graphics;

import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glShaderSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public final class Shader {
	protected final int shaderID;

	public Shader(String file, int glShaderType) throws FileNotFoundException {
		File path = new File(file);
		Scanner sc = new Scanner(path);
		StringBuilder data = new StringBuilder();
		
		if(path.exists()) {
			while(sc.hasNextLine()) {
				data.append(sc.nextLine());
				data.append('\n');
			}
			
			sc.close();
		}
		
		shaderID = glCreateShader(glShaderType);
		glShaderSource(shaderID, data);
		glCompileShader(shaderID);
	}
	
	public int getShaderID() {
		return shaderID;
	}
	
	public void delete() {
		glDeleteShader(shaderID);
	}
}
