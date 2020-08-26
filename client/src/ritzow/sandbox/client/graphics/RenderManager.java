package ritzow.sandbox.client.graphics;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.data.StandardClientProperties;
import ritzow.sandbox.client.graphics.ModelStorage.ModelBuffers;
import ritzow.sandbox.client.graphics.ModelStorage.ModelData;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.client.graphics.TextureAtlas.AtlasRegion;
import ritzow.sandbox.client.ui.Font;
import ritzow.sandbox.client.util.ClientUtility;
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

	public static final Cleaner CLEANER = Cleaner.create();

	public static final int[] VERTEX_INDICES_RECT = {0, 1, 2, 0, 2, 3};

	public static Font FONT; //TODO put this somewhere else?

	public static ModelRenderer MODEL_RENDERER;
	public static ShadingComputeProgram SHADING_PROGRAM;
	public static ShadingApplyProgram SHADING_APPLY_PROGRAM;
	public static BlockRenderProgram BLOCK_RENDERER;

	public static LightRenderProgram LIGHT_RENDERER;
	public static FullscreenQuadProgram FULLSCREEN_RENDERER;
	public static LightingApplyProgram LIGHT_APPLY_RENDERER;

	public static void setup() throws IOException {
		log().info("Loading OpenGL");
		if(RenderManager.OPENGL_CAPS.GL_ARB_debug_output && StandardClientOptions.USE_OPENGL_4_6 && StandardClientOptions.DEBUG_OPENGL) {
			glEnable(GL_DEBUG_OUTPUT);
			glDebugMessageCallback(RenderManager::debugCallback, 0);
		}

		glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		glDisable(GL_MULTISAMPLE);
		glEnablei(GL_BLEND, RenderManager.MAIN_DRAW_BUFFER_INDEX);
		//TODO vsync doesnt work in fullscreen

		TextureData
			dirt = Textures.loadTextureName("dirt"),
			grass = Textures.loadTextureName("grass"),
			glass = Textures.loadTextureName("glass"),
			face = Textures.loadTextureName("greenFace"),
			red = Textures.loadTextureName("redSquare"),
			sky = Textures.loadTextureName("clouds_online"),
			blue = Textures.loadTextureName("blueSquare"),
			night = Textures.loadTextureName("night"),
			stickman_stand = Textures.loadTextureName("stickman"),
			stickman_crouch = Textures.loadTextureName("stickman_crouch"),
			fontTexture = Textures.loadTextureData(StandardClientProperties.ASSETS_PATH.resolve("fonts").resolve("default").resolve("sheet01.png"));

		//TODO implement half-pixel correction to prevent bleeding and the weird artifacting stuff
		//https://gamedev.stackexchange.com/a/49585
		TextureAtlas atlas = Textures.buildAtlas(sky, grass, dirt, glass, face, red, blue, fontTexture, stickman_stand, stickman_crouch, night);

		//TODO look into using https://github.com/javagl/JglTF with Blender
		List<ModelData> models = new ArrayList<>(List.of(
			textureToModelFit(model -> GameModels.MODEL_DIRT_BLOCK = model, dirt, atlas, 1),
			textureToModelFit(model -> GameModels.MODEL_GRASS_BLOCK = model, grass, atlas, 1),
			textureToModelFit(model -> GameModels.MODEL_GREEN_FACE = model, face, atlas, 1),
			textureToModelFit(model -> GameModels.MODEL_RED_SQUARE = model, red, atlas, 1),
			textureToModelFit(model -> GameModels.MODEL_SKY = model, sky, atlas, 1),
			textureToModelFit(model -> GameModels.MODEL_BLUE_SQUARE = model, blue, atlas, 1),
			textureToModelFit(model -> GameModels.MODEL_NIGHT_SKY = model, night, atlas, 1),
			textureToModelFit(model -> GameModels.MODEL_GLASS_BLOCK = model, glass, atlas, 1),
			textureToModelFit(model ->  GameModels.STICKMAN_STAND = model, stickman_stand, atlas, 2),
			textureToModelFit(model ->  GameModels.STICKMAN_CROUCH = model, stickman_crouch, atlas, 1),
			new ModelData(model -> GameModels.MODEL_ATLAS = model, 1, 1, TextureAtlas.NORMAL_POS, TextureAtlas.ATLAS_COORDS, VERTEX_INDICES_RECT)
		));

		AtlasRegion region = atlas.getRegion(fontTexture);
		FONT = Font.load(1.0d / atlas.width(), 25, 9, 9, 1, fontTexture.getWidth(), fontTexture.getHeight(), models::add,
			VERTEX_INDICES_RECT, region.leftX(), region.bottomY(), region.rightX(), region.topY());

		int textureAtlas = atlas.texture();

		ModelBuffers modelStorage = ModelStorage.uploadModels(models);

		if(StandardClientOptions.USE_OPENGL_4_6) {
			SHADING_PROGRAM = new ShadingComputeProgram(
				spirv("shading.comp.spv", ShaderType.COMPUTE)
			);

			SHADING_APPLY_PROGRAM = new ShadingApplyProgram(
				spirv("fullscreen.vert.spv", ShaderType.VERTEX),
				spirv("shading_apply.geom.spv", ShaderType.GEOMETRY),
				spirv("shading_apply.frag.spv", ShaderType.FRAGMENT)
			);

			MODEL_RENDERER = new ModelRenderProgramEnhanced(
				spirv("model.vert.spv", ShaderType.VERTEX),
				spirv("model.frag.spv", ShaderType.FRAGMENT),
				textureAtlas,
				modelStorage
			);

			BLOCK_RENDERER = new BlockRenderProgram(
				spirv("blocks.vert.spv", ShaderType.VERTEX),
				spirv("blocks.frag.spv", ShaderType.FRAGMENT),
				modelStorage,
				textureAtlas
			);
		} else {
			MODEL_RENDERER = new ModelRenderProgramOld(
				source("model.vert", ShaderType.VERTEX),
				source("model.frag", ShaderType.FRAGMENT),
				textureAtlas,
				modelStorage
			);
		}

		GraphicsUtility.checkErrors();
	}

	private static ModelData textureToModelFit(ModelDestination dest, TextureData texture, TextureAtlas atlas, float fitDimension) {
		float scale = ClientUtility.scaleToFit(fitDimension, fitDimension, texture.getWidth(), texture.getHeight());
		return textureToModel(dest, texture, atlas, texture.getWidth() * scale, texture.getHeight() * scale);
	}

	private static ModelData textureToModelScale(ModelDestination dest, TextureData texture, TextureAtlas atlas, float scale) {
		return textureToModel(dest, texture, atlas, texture.getWidth() * scale, texture.getHeight() * scale);
	}

	private static ModelData textureToModel(ModelDestination dest, TextureData texture, TextureAtlas atlas) {
		return textureToModel(dest, texture, atlas, texture.getWidth(), texture.getHeight());
	}

	private static ModelData textureToModel(ModelDestination dest, TextureData texture, TextureAtlas atlas, float width, float height) {
		float halfWidth = width / 2f;
		float halfHeight = height / 2f;
		float[] positions = {
			-halfWidth, halfHeight,
			-halfWidth, -halfHeight,
			halfWidth, -halfHeight,
			halfWidth, halfHeight
		};
		return new ModelData(dest, width, height, positions, atlas.getRegion(texture).toTextureCoordinates(), VERTEX_INDICES_RECT);
	}

	private static Shader source(String file, ShaderType type) throws IOException {
		return Shader.fromSource(Files.readString(SHADERS_PATH.resolve("old").resolve(file)), type);
	}

	private static Shader spirv(String file, ShaderType type) throws IOException {
		return Shader.fromSPIRV(Utility.load(SHADERS_PATH.resolve("new").resolve("out").resolve(file), BufferUtils::createByteBuffer), type);
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

	public static void setStandardBlending() {
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glBlendEquation(GL_FUNC_ADD);
	}

	public static void setViewport(int width, int height) {
		glViewport(0, 0, width, height);
	}

	public static void postRender(Display display) {
		glFlush();
		glFinish();
		GraphicsUtility.checkErrors();
		display.refresh();
		//TODO call GameState.display().refresh()?
	}
}
