package world.entity.component;

import java.io.Serializable;

public final class Scale implements Serializable {
	private static final long serialVersionUID = -6051593995919509687L;
	
	private float x;
	private float y;
	
	public Scale(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
	public void setX(float x) {
		this.x = x;
	}
	
	public void setY(float y) {
		this.y = y;
	}
}
