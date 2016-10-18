package world.entity;

import graphics.Model;
import graphics.Renderer;
import world.entity.component.Graphics;

public class GraphicsEntity extends Entity {
	private static final long serialVersionUID = 6938705771360005876L;
	
	protected final transient Graphics graphics;
	
	public GraphicsEntity(Model model) {
		this.graphics = new Graphics(model);
	}
	
	public Graphics getGraphics() {
		return graphics;
	}
	
	public void update(float milliseconds) {
		super.update(milliseconds);
		graphics.getRotation().update(milliseconds);
	}
	
	public void render(Renderer renderer) {
		renderer.loadOpacity(graphics.getOpacity());
		renderer.loadTransformationMatrix(positionX, positionY, graphics.getScale().getX(), graphics.getScale().getY(), graphics.getRotation().getRotation());
		graphics.getModel().render();
	}

}
