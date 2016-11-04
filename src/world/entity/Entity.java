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
	protected float width;
	protected float height;
	protected float mass;
	protected float friction;
	protected boolean doCollision;
	protected boolean doBlockCollisionResolution;
	protected boolean doEntityCollisionResolution;
	protected boolean shouldDelete;
	
	public Entity() {
		positionX = 0;
		positionY = 0;
		velocityX = 0;
		velocityY = 0;
		friction = 0;
		width = 1;
		height = 1;
		mass = 1;
		doCollision = true;
		doBlockCollisionResolution = true;
		doEntityCollisionResolution = true;
		shouldDelete = false;
	}
	
	public abstract void render(ModelRenderer renderer);
	public abstract void onCollision(World world, Entity e);
	
	public void update(float milliseconds) {
		positionX += velocityX * milliseconds;
		positionY += velocityY * milliseconds;
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

	public final float getWidth() {
		return width;
	}

	public final float getHeight() {
		return height;
	}

	public final float getMass() {
		return mass;
	}

	public final float getFriction() {
		return friction;
	}
	
	public final boolean getShouldDelete() {
		return shouldDelete;
	}
	
	public final boolean getDoCollision() {
		return doCollision;
	}

	public final boolean getDoBlockCollisionResolution() {
		return doBlockCollisionResolution;
	}
	
	public final boolean getDoEntityCollisionResolution() {
		return doEntityCollisionResolution;
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

	public final void setWidth(float width) {
		this.width = width;
	}

	public final void setHeight(float height) {
		this.height = height;
	}

	public final void setMass(float mass) {
		this.mass = mass;
	}

	public final void setFriction(float friction) {
		this.friction = friction;
	}

	public final void setDoBlockCollisionResolution(boolean doBlockCollision) {
		this.doBlockCollisionResolution = doBlockCollision;
	}
	
	public final void setDoEntityCollisionResolution(boolean doEntityCollisionResolution) {
		this.doEntityCollisionResolution = doEntityCollisionResolution;
	}
	
}