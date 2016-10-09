package world.entity;

import graphics.Model;
import graphics.Renderer;
import world.entity.component.visual.Graphics;

public class GenericEntity extends Entity {
	private static final long serialVersionUID = 6938705771360005876L;
	
	protected final Graphics graphics;
	
	public GenericEntity(Model model) {
		this.graphics = new Graphics(model);
	}
	
	public Graphics getGraphics() {
		return graphics;
	}
	
	public void update(float milliseconds) {
		super.update(milliseconds);
		graphics.rotation().update(milliseconds);
	}
	
	public void render(Renderer renderer) {
		renderer.loadOpacity(graphics.getOpacity());
		renderer.loadTransformationMatrix(position.getX(), position.getY(), graphics.scale().getX(), graphics.scale().getY(), graphics.rotation().getRotation());
		graphics.getModel().render();
	}

}
