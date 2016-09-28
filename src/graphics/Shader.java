package graphics;

import static org.lwjgl.opengl.GL20.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Shader {
	
	protected int shaderID;

	public Shader(String file, int glShaderType) {
		try {
			File path = new File(file);
			Scanner sc = new Scanner(path);
			StringBuilder data = new StringBuilder();
			
			if(path.exists()) {
				while(sc.hasNextLine()) {
					data.append(sc.nextLine() + "\n");
				}
				
				sc.close();
			}
			
			shaderID = glCreateShader(glShaderType);
			glShaderSource(shaderID, data);
			glCompileShader(shaderID);
		}
		
		catch (FileNotFoundException e) {
			e.printStackTrace();
			shaderID = -1;
		}
	}
	
	public int getShaderID() {
		return shaderID;
	}
	
	public void delete() {
		glDeleteShader(shaderID);
	}
}
