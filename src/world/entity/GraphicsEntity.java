package world.entity;

import graphics.Model;
import graphics.ModelRenderer;
import world.World;
import world.entity.component.Graphics;

public class GraphicsEntity extends Entity {
	private static final long serialVersionUID = 6938705771360005876L;
	
	protected final transient Graphics graphics;
	
	protected float width;
	protected float height;
	protected float mass;
	
	public GraphicsEntity(Model model, float width, float height, float mass) {
		this.graphics = new Graphics(model);
		this.width = width;
		this.height = height;
		this.mass = mass;
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
	public void onCollision(World world, Entity e, float time) {
		
	}

	@Override
	public boolean getShouldDelete() {
		return false;
	}

	@Override
	public boolean getDoCollision() {
		return true;
	}

	@Override
	public boolean getDoBlockCollisionResolution() {
		return true;
	}

	@Override
	public boolean getDoEntityCollisionResolution() {
		return true;
	}

	@Override
	public float getFriction() {
		return 0.05f;
	}

	@Override
	public float getWidth() {
		return width;
	}

	@Override
	public float getHeight() {
		return height;
	}

	@Override
	public float getMass() {
		return mass;
	}

}
