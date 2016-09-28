package world.entity.component.spacial;

public final class Hitbox {
	private float width;
	private float height;
	private float priority;
	
	public Hitbox(float width, float height) {
		this.width = width;
		this.height = height;
		this.priority = 0;
	}
	
	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}
	
	public void setWidth(float width) {
		this.width = width;
	}

	public void setHeight(float height) {
		this.height = height;
	}
	
	public float getPriority() {
		return priority;
	}
	
	public void setPriority(float priority) {
		this.priority = priority;
	}
}
