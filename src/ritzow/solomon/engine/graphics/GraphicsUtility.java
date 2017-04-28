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

import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;

public final class GraphicsUtility {
	
	private GraphicsUtility() {}
	
	private static final Map<Integer, String> errorMessages;
	
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
	
	public static void checkFramebufferCompleteness(Framebuffer... framebuffers) {
		for(Framebuffer f : framebuffers) {
			checkFramebufferCompleteness(f);
		}
	}
	
	public static void checkFramebufferCompleteness(Framebuffer framebuffer) {
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer.framebufferID);
		int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
	    if (status != GL_FRAMEBUFFER_COMPLETE) {
	      	throw new OpenGLException("Framebuffer incomplete: " + status);
	    }
	}
	
	public static void checkShaderCompilation(Shader...shaders) {
		for(Shader s : shaders) {
			checkShaderCompilation(s);
		}
	}
	
	public static void checkShaderCompilation(Shader shader) {
		if(glGetShaderi(shader.getShaderID(), GL_COMPILE_STATUS) != 1) {
			throw new OpenGLException("Shader Compilation Error: " + glGetShaderInfoLog(shader.getShaderID()));
		}
	}
	
	public static void checkProgramCompilation(ShaderProgram... programs) {
		for(ShaderProgram p : programs) {
			checkProgramCompilation(p);
		}
	}
	
	public static void checkProgramCompilation(ShaderProgram program) {
		if(glGetProgrami(program.programID, GL_LINK_STATUS) != 1) {
			throw new OpenGLException("Shader program failed linking: " + glGetProgramInfoLog(program.programID));
		}
	}
	
	/**
	 * glValidateProgram() is meant to be called directly before a draw call 
	 * with that shader bound and all the bindings (VAO, textures) set. Its 
	 * purpose is to ensure that the shader can execute given the current GL 
	 * state. So, you should not be calling it as part of your shader initialization. 
	 * @param program the program to validate
	 */
	public static void checkProgramValidation(ShaderProgram program) {
		if(glGetProgrami(program.programID, GL_VALIDATE_STATUS) != 1) {
			throw new OpenGLException("Shader program failed validation:\n" + glGetProgramInfoLog(program.programID));
		}
	}
}
