package ritzow.sandbox.client.graphics;

/**
 * A Graphics instance represents a model ID, opacity, scale, and rotation
 * @author Solomon Ritzow
 *
 */
public interface Graphics {
	public int getModelID();
	public float getOpacity();
	public float getScaleX();
	public float getScaleY();
	public float getRotation();
}
