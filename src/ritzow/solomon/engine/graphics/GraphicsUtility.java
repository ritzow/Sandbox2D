package ritzow.solomon.engine.graphics;

import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VALIDATE_STATUS;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;

import java.util.HashMap;
import java.util.Map;

public class GraphicsUtility {
	
	private static final Map<Integer, String> errorMessages;;
	
	static {
		errorMessages = new HashMap<Integer, String>();
		errorMessages.put(0x500, "Invalid enum");
		errorMessages.put(0x501, "Invalid value");
		errorMessages.put(0x502, "Invalid operation");
		errorMessages.put(0x506, "Attempt to use incomplete framebuffer");
		errorMessages.put(0x507, "OpenGL context lost");
	}
	
	public static void checkErrors() {
		int error = org.lwjgl.opengl.GL11.glGetError();
		if(error != 0) {
			StringBuilder errorMsg = new StringBuilder("Error Codes: ");
			errorMsg.append(error);
			while((error = org.lwjgl.opengl.GL11.glGetError()) != 0) {
				errorMsg.append(", ");
				errorMsg.append(error);
				errorMsg.append(':');
				errorMsg.append(errorMessages.get(error));
			}
			throw new OpenGLException(errorMsg.toString());
		}
	}

	public static void checkFramebufferCompleteness(int framebuffer) {
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		
	    if (status != GL_FRAMEBUFFER_COMPLETE) {
	      	System.out.println("OpenGL Framebuffer Error: " + status);
	    }
	}
	
	public static void printShaderCompilation(Shader... shaders) {
		for(Shader s : shaders) {
			boolean compiled = glGetShaderi(s.getShaderID(), GL_COMPILE_STATUS) == 1;
			System.out.println("Shader ID " + s.getShaderID() + (compiled ? " compiled" : " failed to compile"));
			if(!compiled) {
				System.err.println(glGetShaderInfoLog(s.getShaderID()));
			}
		}
	}

	public static void printProgramCompilation(ShaderProgram... programs) {
		for(ShaderProgram p : programs) {
			System.out.println("Shader Program " + p.programID + (glGetProgrami(p.programID, GL_LINK_STATUS) == 1 ? " linked" : " failed linking"));
			System.out.println("Shader Program " + p.programID + (glGetProgrami(p.programID, GL_VALIDATE_STATUS) == 1 ? " validated" : " failed validation"));
		}
	}
}
