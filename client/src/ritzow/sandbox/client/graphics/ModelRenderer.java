package ritzow.sandbox.client.graphics;

import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.ModelRenderProgramBase.ModelData;

public interface ModelRenderer {

	static ModelRenderer create(Shader vertexShader, Shader fragmentShader, int textureAtlas, ModelData... models) {
		return StandardClientOptions.USE_OPENGL_4_6 ?
				   new ModelRenderProgramEnhanced(vertexShader, fragmentShader, textureAtlas, models) :
				   new ModelRenderProgramOld(vertexShader, fragmentShader, textureAtlas, models);
	}

	void setCurrent();
	void loadViewMatrixStandard(int framebufferWidth, int framebufferHeight);
	void loadViewMatrix(Camera camera, int framebufferWidth, int framebufferHeight);
	void queueRender(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation);
	void flush();
	void delete();
}
