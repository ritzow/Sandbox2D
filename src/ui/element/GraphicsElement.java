package ui.element;

import graphics.Model;
import graphics.ModelRenderer;
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
	
	public Graphics getGraphics() {
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
	public void render(ModelRenderer renderer, float x, float y) {
		renderer.loadTransformationMatrix(x + offsetX, y + offsetY, graphics.getScaleX(), graphics.getScaleY(), graphics.getRotation());
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
