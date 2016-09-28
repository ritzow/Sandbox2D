package graphics.ui.element;

import graphics.Model;
import graphics.Renderer;
import world.entity.component.spacial.Position;
import world.entity.component.visual.Graphics;

public class GraphicsElement extends Element {
	
	protected final Graphics graphics;
	protected final Position offset;
	
	public GraphicsElement(Model model, float offsetX, float offsetY) {
		graphics = new Graphics(model);
		offset = new Position(offsetX, offsetY);
	}
	
	public Graphics graphics() {
		return graphics;
	}
	
	public Position offset() {
		return offset;
	}

	@Override
	public void render(Renderer renderer, float x, float y) {
		renderer.loadTransformation(renderer.getTransformationMatrix(x + offset.getX(), y + offset.getY(), graphics.scale().getX(), graphics.scale().getY(), graphics.rotation().getRotation()));
		renderer.loadOpacity(graphics.getOpacity());
		graphics.getModel().render();
	}
}
