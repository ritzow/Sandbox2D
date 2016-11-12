package world.entity;

import graphics.ModelRenderer;
import world.World;

import java.io.Serializable;

public abstract class Entity implements Serializable {
	private static final long serialVersionUID = 7177412000462430179L;
	
	protected float positionX;
	protected float positionY;
	protected float velocityX;
	protected float velocityY;
	
	public void update(float time) {
		positionX += velocityX * time;
		positionY += velocityY * time;
	}
	
	public abstract void render(ModelRenderer renderer);
	public abstract void onCollision(World world, Entity e, float time);

	public abstract boolean getShouldDelete();
	public abstract boolean getDoCollision();
	public abstract boolean getDoBlockCollisionResolution();
	public abstract boolean getDoEntityCollisionResolution();
	public abstract float getFriction();
	public abstract float getWidth();
	public abstract float getHeight();
	public abstract float getMass();
	
	public final float getSpeed() {
		return (float)Math.abs(Math.sqrt(velocityX * velocityX + velocityY * velocityY));
	}

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
	
}