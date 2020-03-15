package ritzow.sandbox.client.graphics;

/**
 * A Graphics instance represents a model ID, opacity, scale, and rotation
 * @author Solomon Ritzow
 *
 */
public interface Graphics {
	int getModelID();
	float getOpacity();
	float getScaleX();
	float getScaleY();
	float getRotation();
}
