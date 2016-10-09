package world.entity;

import graphics.Renderer;
import java.io.Serializable;
import world.entity.component.spacial.Hitbox;
import world.entity.component.spacial.Position;
import world.entity.component.spacial.Velocity;

public abstract class Entity implements Serializable {
	private static final long serialVersionUID = 7177412000462430179L;
	
	protected final Position position;
	protected final Velocity velocity;
	protected final Hitbox hitbox;
	
	public Entity() {
		this.position = new Position(0, 0);
		this.velocity = new Velocity(0, 0);
		this.hitbox = new Hitbox(1, 1);
	}
	
	public void update(float milliseconds) {
		velocity.update(milliseconds);
		position.update(velocity, milliseconds);
	}
	
	public abstract void render(Renderer renderer);
	
	public Position position() {
		return position;
	}
	
	public Velocity velocity() {
		return velocity;
	}
	
	public Hitbox getHitbox() {
		return hitbox;
	}
}