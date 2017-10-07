package ritzow.sandbox.world.component;

public interface Physical {
	public float getWidth();
	public float getHeight();
	public boolean collidesWith(Physical o);
}
