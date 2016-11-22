package ui.element;

import graphics.Model;
import graphics.ModelRenderer;
import world.entity.component.Graphics;

public class GraphicsElement extends Element {
	
	protected final Graphics graphics;
	
	public GraphicsElement(Model model) {
		graphics = new Graphics(model);
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
