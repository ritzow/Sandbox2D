package ritzow.solomon.engine.world.component;

public interface Physical {
	public float getWidth();
	public float getHeight();
	public boolean collidesWith(Physical o);
}
