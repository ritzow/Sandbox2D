package world.entity;

import graphics.Renderer;
import java.io.Serializable;
import world.entity.component.spacial.Hitbox;

public abstract class Entity implements Serializable {
	private static final long serialVersionUID = 7177412000462430179L;
	
	protected final Hitbox hitbox;
	
	protected float positionX;
	protected float positionY;
	protected float velocityX;
	protected float velocityY;
	
	public Entity() {
		positionX = 0;
		positionY = 0;
		this.hitbox = new Hitbox(1, 1, 0.0f);
	}
	
	public void update(float milliseconds) {
		positionX += velocityX * milliseconds;
		positionY += velocityY * milliseconds;
	}
	
	public abstract void render(Renderer renderer);
	
	public final float getPositionX() {
		return positionX;
	}

	public final float getPositionY() {
		return positionY;
	}

	public final float getVelocityX() {
		return velocityX;
	}

	public final float getVelocityY() {
		return velocityY;
	}

	public final void setPositionX(float positionX) {
		this.positionX = positionX;
	}

	public final void setPositionY(float positionY) {
		this.positionY = positionY;
	}

	public final void setVelocityX(float velocityX) {
		this.velocityX = velocityX;
	}

	public final void setVelocityY(float velocityY) {
		this.velocityY = velocityY;
	}

	public Hitbox getHitbox() {
		return hitbox;
	}
}