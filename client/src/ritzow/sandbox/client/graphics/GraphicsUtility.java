package ritzow.sandbox.client.graphics;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.lwjgl.opengl.GL46C.*;

public final class GraphicsUtility {

	private GraphicsUtility() {}

	private static final Map<Integer, String> errorMessages = Map.ofEntries(
		Map.entry(0x500, "Invalid enum"),
		Map.entry(0x501, "Invalid value"),
		Map.entry(0x502, "Invalid operation"),
		Map.entry(0x506, "Attempt to use incomplete framebuffer"),
		Map.entry(0x507, "OpenGL context lost")
	);

	/** Creates an fixed-size texture with the provided data **/
	public static int uploadTextureData(ByteBuffer pixels, int width, int height) {
		int id = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, id);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexStorage2D(GL_TEXTURE_2D, 1, GL_SRGB8_ALPHA8, width, height);
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
		glBindTexture(GL_TEXTURE_2D, 0);
		GraphicsUtility.checkErrors();
		return id;
	}

	public static void checkErrors() throws OpenGLException {
		int error = org.lwjgl.opengl.GL11.glGetError();
		if(error != 0) {
			throw new OpenGLException("Error Code: " + error + " (" + errorMessages.get(error) + ")");
		}
	}

	public static void checkFramebufferCompleteness(int framebuffer) {
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
	    if (status != GL_FRAMEBUFFER_COMPLETE) {
	      	throw new OpenGLException("OpenGL Framebuffer Error: " + status);
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
			String error = glGetShaderInfoLog(shader.getShaderID());
			glDeleteShader(shader.getShaderID());
			throw new OpenGLException("Shader Compilation Error" + (error.length() > 0 ? ": " + error : ""));
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
