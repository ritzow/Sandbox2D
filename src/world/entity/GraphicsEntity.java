package world.entity;

import graphics.Model;
import graphics.ModelRenderer;
import world.World;
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
		graphics.setRotation(graphics.getRotation() + graphics.getRotationVelocity());
	}
	
	public void render(ModelRenderer renderer) {
		graphics.render(renderer, positionX, positionY);
	}

	@Override
	public void onCollision(World world, Entity e) {
		
	}

}
