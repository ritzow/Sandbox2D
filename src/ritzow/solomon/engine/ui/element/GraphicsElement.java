package ritzow.solomon.engine.ui.element;

import ritzow.solomon.engine.graphics.Graphics;
import ritzow.solomon.engine.graphics.Model;
import ritzow.solomon.engine.graphics.ModelRenderer;
import ritzow.solomon.engine.graphics.MutableGraphics;

public class GraphicsElement extends Element {
	
	protected final MutableGraphics graphics;
	
	public GraphicsElement(Model model) {
		graphics = new MutableGraphics(model);
	}
	
	public Graphics getGraphics() {
		return graphics;
	}

	@Override
	public void render(ModelRenderer renderer) {
		renderer.loadTransformationMatrix(positionX, positionY, graphics.getScaleX(), graphics.getScaleY(), graphics.getRotation());
		renderer.loadOpacity(graphics.getOpacity());
		graphics.getModel().render();
	}

	@Override
	public float getWidth() {
		return graphics.getScaleX();
	}

	@Override
	public float getHeight() {
		return graphics.getScaleY();
	}
}
