package ritzow.sandbox.client.graphics;

import ritzow.sandbox.client.graphics.ModelStorage.ModelBuffers;
import ritzow.sandbox.client.util.ClientUtility;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL15C.GL_TEXTURE0;

public abstract class ModelRenderProgramBase extends ShaderProgram implements ModelRenderer {
	private static final int ATLAS_TEXTURE_UNIT = GL_TEXTURE0;

	protected final float[] aspectMatrix = {
		1, 0, 0, 0,
		0, 1, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, 1,
	};

	protected final float[] viewMatrix = {
		0, 0, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, 0,
		0, 0, 0, 1,
	};

	protected final int uniform_view, vaoID, atlasTexture;

	public ModelRenderProgramBase(Shader vertex, Shader fragment, int textureAtlas, ModelBuffers models) {
		super(vertex, fragment);
		this.uniform_view = getUniformLocation("view");
		this.atlasTexture = textureAtlas;
		this.vaoID = ModelStorage.defineVAO(models, getAttributeLocation("position"), getAttributeLocation("textureCoord"));
		setInteger(getUniformLocation("atlasSampler"), ATLAS_TEXTURE_UNIT);
	}

	@Override
	public void prepare() {
		setCurrent();
		glActiveTexture(ATLAS_TEXTURE_UNIT);
		glBindTexture(GL_TEXTURE_2D, atlasTexture);
		//TODO maybe move this to client code for more control ie when rendering lights
		//set the blending mode to allow transparency
	}

	@Override
	public void loadViewMatrixIdentity() {
		flush();
		aspectMatrix[0] = 1;
		aspectMatrix[5] = 1;
		setMatrix(uniform_view, aspectMatrix);
	}

	@Override
	public void loadViewMatrixScale(float guiScale, int framebufferWidth, int framebufferHeight) {
		flush();
		GraphicsUtility.checkErrors();
		aspectMatrix[0] = guiScale/framebufferWidth;
		aspectMatrix[5] = guiScale/framebufferHeight;
		setMatrix(uniform_view, aspectMatrix);
		GraphicsUtility.checkErrors();
	}

	@Override
	public void loadViewMatrixStandard(int framebufferWidth, int framebufferHeight) {
		flush();
		aspectMatrix[0] = framebufferHeight/(float)framebufferWidth;
		aspectMatrix[5] = 1;
		setMatrix(uniform_view, aspectMatrix);
	}

	@Override
	public void loadViewMatrix(Camera camera, int framebufferWidth, int framebufferHeight) {
		flush();
		//TODO add camera rotation
		ClientUtility.setViewMatrix(viewMatrix, camera, framebufferWidth, framebufferHeight);
		setMatrix(uniform_view, viewMatrix);
	}
}
