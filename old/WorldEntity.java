

import world.capability.*;
import world.entity.component.*;
import world.entity.component.Velocity;
import world.entity.component.visual.Scale;

public class WorldEntity extends Entity implements Positionable, Physical {
	protected float accelerationRotation;
	protected float collisionPriority;
	protected float mass;
	
	protected final Position position;
	protected final Hitbox hitbox;
	protected final Velocity velocity;
	
	public void update() {
		this.velocity.update();
		this.position.setX(this.position.getX() + this.velocity.getX());
		this.position.setY(this.position.getY() + this.velocity.getY());
	}
	
	public Position position() {
		return position;
	}
	
	public Hitbox hitbox() {
		return hitbox;
	}
	
	public Velocity velocity() {
		return velocity;
	}
	
	public final float getCollisionPriority() {
		return collisionPriority;
	}

	public final void setCollisionPriority(float collisionPriority) {
		this.collisionPriority = collisionPriority;
	}
	
	public float getMass() {
		return mass;
	}
	
	public void setMass(float mass) {
		this.mass = mass;
	}
}