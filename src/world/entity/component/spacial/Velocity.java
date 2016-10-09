package world.entity.component.spacial;

import world.entity.component.Vector;

public final class Velocity extends Vector {
	private static final long serialVersionUID = -7193764413143491680L;
	
	private float accelerationX;
	private float accelerationY;

	public Velocity(float x, float y) {
		super(x, y);
	}
	
	public void update(float milliseconds) {
		this.x += accelerationX * milliseconds;
		this.y += accelerationY * milliseconds;
	}
	
	public float getAccelerationX() {
		return accelerationX;
	}
	
	public float getAccelerationY() {
		return accelerationY;
	}
	
	public void setAccelerationX(float acceleration) {
		this.accelerationX = acceleration;
	}
	
	public void setAccelerationY(float acceleration) {
		this.accelerationY = acceleration;
	}
	
}
