package ritzow.sandbox.world.component;

public interface Physical {
	float getWidth();
	float getHeight();
	boolean collidesWith(Physical o);
}
