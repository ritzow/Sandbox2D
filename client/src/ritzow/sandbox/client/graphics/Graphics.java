package ritzow.sandbox.client.graphics;

/**
 * A Graphics instance represents a model ID, opacity, scale, and rotation
 * @author Solomon Ritzow
 *
 */
public interface Graphics {
	Model getModel();
	float getOpacity();
	float getScaleX();
	float getScaleY();
	float getRotation();
}
