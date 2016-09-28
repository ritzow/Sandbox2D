package world.entity.component.visual;

public final class Rotation {
	private float rotation;
	private float velocity;
	
	public Rotation(float radians) {
		this.rotation = radians;
	}
	
	public void update(float milliseconds) {
		this.rotation += velocity * milliseconds;
	}
	
	public float getRotation() {
		return rotation;
	}
	
	public void setRotation(float rotation) {
		this.rotation = rotation;
	}
	
	public float getVelocity() {
		return velocity;
	}
	
	public void setVelocity(float velocity) {
		this.velocity = velocity;
	}
}
