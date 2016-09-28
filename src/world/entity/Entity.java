package world.entity;

import graphics.Model;
import graphics.Renderer;
import world.entity.component.spacial.Hitbox;
import world.entity.component.spacial.Position;
import world.entity.component.spacial.Velocity;
import world.entity.component.visual.Graphics;

public class Entity {
	protected final Position position;
	protected final Velocity velocity;
	protected final Graphics graphics;
	protected final Hitbox hitbox;
	
	public Entity(Model model) {
		this.position = new Position(0, 0);
		this.velocity = new Velocity(0, 0);
		this.graphics = new Graphics(model);
		this.hitbox = new Hitbox(1, 1);
	}
	
	public void update(float milliseconds) {
		velocity.update(milliseconds);
		position.update(velocity, milliseconds);
		graphics.rotation().update(milliseconds);
	}
	
	public Position position() {
		return position;
	}
	
	public Velocity velocity() {
		return velocity;
	}
	
	public Graphics graphics() {
		return graphics;
	}
	
	public Hitbox hitbox() {
		return hitbox;
	}
	
	public void render(Renderer renderer) {
		renderer.loadOpacity(graphics.getOpacity());
		renderer.loadTransformation(renderer.getTransformationMatrix(position.getX(), position.getY(), graphics.scale().getX(), graphics.scale().getY(), graphics.rotation().getRotation()));
		graphics.getModel().render();
	}
}