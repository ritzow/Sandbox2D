package ritzow.sandbox.client.graphics;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.data.StandardClientProperties;
import ritzow.sandbox.client.graphics.ModelRenderProgramBase.ModelData;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.client.ui.Font;
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

	public static Font FONT; //TODO put this somewhere else?

	public static ModelRenderer setup() throws IOException {
		log().info("Loading OpenGL");
		GL.create();
		RenderManager.OPENGL_CAPS = GL.createCapabilities(true);
		if(RenderManager.OPENGL_CAPS.GL_ARB_debug_output && StandardClientOptions.USE_OPENGL_4_6 && StandardClientOptions.DEBUG) {
			glEnable(GL_DEBUG_OUTPUT);
			glDebugMessageCallback(RenderManager::debugCallback, 0);
		}
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_MULTISAMPLE);
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
			sky = Textures.loadTextureName("clouds_online"),
			fontTexture = Textures.loadTextureData(StandardClientProperties.ASSETS_PATH.resolve("fonts").resolve("default").resolve("sheet01.png"));

		//TODO implement half-pixel correction to prevent bleeding and the weird artifacting stuff
		//https://gamedev.stackexchange.com/a/49585
		TextureAtlas atlas = Textures.buildAtlas(sky, grass, dirt, face, red, fontTexture);

		//TODO look into using https://github.com/javagl/JglTF with Blender
		List<ModelData> models = new ArrayList<>(List.of(
			new ModelData(GameModels.MODEL_DIRT_BLOCK, positions, atlas.getCoordinates(dirt), indices),
			new ModelData(GameModels.MODEL_GRASS_BLOCK, positions, atlas.getCoordinates(grass), indices),
			new ModelData(GameModels.MODEL_GREEN_FACE, positions, atlas.getCoordinates(face), indices),
			new ModelData(GameModels.MODEL_RED_SQUARE, positions, atlas.getCoordinates(red), indices),
			new ModelData(GameModels.MODEL_SKY, positions, atlas.getCoordinates(sky), indices)
		));

		float[] fontCoords = atlas.getCoordinates(fontTexture);
		FONT = Font.load(25, 9, 9, 1, fontTexture.getWidth(), fontTexture.getHeight(), models,
			positions, indices, /*left x */ fontCoords[0], /* bottom y */ fontCoords[3], /* right x */ fontCoords[4], /* top y */ fontCoords[1]);

		int textureAtlas = atlas.texture();
		if(StandardClientOptions.USE_OPENGL_4_6) {
			return new ModelRenderProgramEnhanced(
				spirv("model.vert.spv", ShaderType.VERTEX),
				spirv("model.frag.spv", ShaderType.FRAGMENT),
			textureAtlas, models.toArray(ModelData[]::new));
		} else {
			return new ModelRenderProgramOld(
				source("model.vert", ShaderType.VERTEX),
				source("model.frag", ShaderType.FRAGMENT),
			textureAtlas, models.toArray(ModelData[]::new));
		}
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

	public static void postRender() {
		glFlush();
		glFinish();
	}
}
