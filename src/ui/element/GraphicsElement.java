package ui.element;

import graphics.Model;
import graphics.Renderer;
import world.entity.component.Graphics;

public class GraphicsElement extends Element {
	
	protected final Graphics graphics;
	
	protected float offsetX;
	protected float offsetY;
	
	public GraphicsElement(Model model, float offsetX, float offsetY) {
		graphics = new Graphics(model);
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}
	
	public Graphics graphics() {
		return graphics;
	}

	public final float getOffsetX() {
		return offsetX;
	}

	public final float getOffsetY() {
		return offsetY;
	}

	public final void setOffsetX(float offsetX) {
		this.offsetX = offsetX;
	}

	public final void setOffsetY(float offsetY) {
		this.offsetY = offsetY;
	}

	@Override
	public void render(Renderer renderer, float x, float y) {
		renderer.loadTransformationMatrix(x + offsetX, y + offsetY, graphics.getScale().getX(), graphics.getScale().getY(), graphics.getRotation().getRotation());
		renderer.loadOpacity(graphics.getOpacity());
		graphics.getModel().render();
	}
}
