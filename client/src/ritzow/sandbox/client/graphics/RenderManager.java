package ritzow.sandbox.client.graphics;

import java.io.IOException;
import java.nio.file.Files;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.ModelRenderProgram.ModelData;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.util.Utility;

import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL46C.*;
import static ritzow.sandbox.client.data.StandardClientProperties.SHADERS_PATH;
import static ritzow.sandbox.client.util.ClientUtility.log;

/** Contains methods for initializing and destroying the OpenGL context and rendering and updating the display **/
public class RenderManager {
	public static final Framebuffer DISPLAY_BUFFER = new Framebuffer(0);
	public static final int MAIN_DRAW_BUFFER_INDEX = 0;
	public static GLCapabilities OPENGL_CAPS;

	public static ModelRenderProgram setup() throws IOException {
		log().info("Loading OpenGL");
		GL.create();
		RenderManager.OPENGL_CAPS = GL.createCapabilities(true);
		if(RenderManager.OPENGL_CAPS.GL_ARB_debug_output && StandardClientOptions.USE_OPENGL_4_6) {
			glEnable(GL_DEBUG_OUTPUT);
			glDebugMessageCallback(RenderManager::debugCallback, 0);
		}
		glDisable(GL_DEPTH_TEST);
		glEnablei(GL_BLEND, RenderManager.MAIN_DRAW_BUFFER_INDEX);
		glfwSwapInterval(0);

		int[] indices = {0, 1, 2, 0, 2, 3};

		float[] positions = {
			-0.5f,	 0.5f,
			-0.5f,	-0.5f,
			0.5f,	-0.5f,
			0.5f,	 0.5f
		};

		TextureData
			dirt = Textures.loadTextureName("dirt"),
			grass = Textures.loadTextureName("grass"),
			face = Textures.loadTextureName("greenFace"),
			red = Textures.loadTextureName("redSquare"),
			sky = Textures.loadTextureName("clouds_online");
		TextureAtlas atlas = Textures.buildAtlas(sky, grass, dirt, face, red);

		//TODO look into using https://github.com/javagl/JglTF with Blender
		ModelData[] models = {
			new ModelData(GameModels.MODEL_DIRT_BLOCK, positions, atlas.getCoordinates(dirt), indices),
			new ModelData(GameModels.MODEL_GRASS_BLOCK, positions, atlas.getCoordinates(grass), indices),
			new ModelData(GameModels.MODEL_GREEN_FACE, positions, atlas.getCoordinates(face), indices),
			new ModelData(GameModels.MODEL_RED_SQUARE, positions, atlas.getCoordinates(red), indices),
			new ModelData(GameModels.MODEL_SKY, positions, atlas.getCoordinates(sky), indices)
		};

		Shader vertex = StandardClientOptions.USE_OPENGL_4_6 ?
			spirv("model.vert.spv", ShaderType.VERTEX) :
			source("model.vert", ShaderType.VERTEX);
		Shader fragment = StandardClientOptions.USE_OPENGL_4_6 ?
			spirv("model.frag.spv", ShaderType.FRAGMENT) :
			source("model.frag", ShaderType.FRAGMENT);

		var program = ModelRenderProgram.create(vertex, fragment, atlas.texture(), models);
		GraphicsUtility.checkErrors();
		return program;
	}

	private static Shader source(String file, ShaderType type) throws IOException {
		return Shader.fromSource(Files.readString(SHADERS_PATH.resolve(file)), type);
	}

	private static Shader spirv(String file, ShaderType type) throws IOException {
		return Shader.fromSPIRV(Utility.load(SHADERS_PATH.resolve(file), BufferUtils::createByteBuffer), type);
	}

	private static void debugCallback(int source, int type, int id, int severity,
			int length, long message, long userParam) {
		if(severity == GL_DEBUG_SEVERITY_NOTIFICATION) {
			log().info(MemoryUtil.memASCII(message, length));
		} else {
			throw new OpenGLException(MemoryUtil.memASCII(message, length));
		}
	}

	public static void closeContext() {
		GraphicsUtility.checkErrors();
		glfwMakeContextCurrent(0);
		OPENGL_CAPS = null;
		GL.destroy();
	}

	private static int fbWidth, fbHeight;

	public static void preRender(int framebufferWidth, int framebufferHeight) {
		if(fbWidth != framebufferWidth || fbHeight != framebufferHeight)
			glViewport(0, 0, fbWidth = framebufferWidth, fbHeight = framebufferHeight);
	}
}
