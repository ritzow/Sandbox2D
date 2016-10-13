package world.entity.component.spacial;

import java.io.Serializable;

public final class Hitbox implements Serializable {
	private static final long serialVersionUID = -5196752210244941936L;
	
	private float width;
	private float height;
	private float priority;
	private float friction;
	
	public Hitbox(float width, float height, float friction) {
		this.width = width;
		this.height = height;
		this.priority = 0;
		this.friction = friction;
	}
	
	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}
	
	public float getFriction() {
		return friction;
	}
	
	public void setWidth(float width) {
		this.width = width;
	}

	public void setHeight(float height) {
		this.height = height;
	}
	
	public void setFriction(float friction) {
		this.friction = friction;
	}
	
	public float getPriority() {
		return priority;
	}
	
	public void setPriority(float priority) {
		this.priority = priority;
	}
}
