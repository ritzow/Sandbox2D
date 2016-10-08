package util;

import static util.MatrixMath.multiply;

import graphics.Camera;

public class RenderUtil {
	public static float[] getTransformationMatrix(float posX, float posY, float scaleX, float scaleY, float rotation) {		
		return new float[] {
				scaleX * (float) Math.cos(rotation), scaleY * (float) Math.sin(rotation), 0, posX,
				scaleX * (float) -Math.sin(rotation), scaleY * (float) Math.cos(rotation), 0, posY,
				0, 0, 0, 0,
				0, 0, 0, 1,
		};
	}
	
	public static float[] getViewMatrix(Camera camera, float aspectRatio) {
		return multiply(getAspectMatrix(aspectRatio), getCameraMatrix(camera));
	}
	
	public static float[] getCameraMatrix(Camera camera) {
		return new float[] {
				camera.getZoom(), 0, 0, -camera.getX() * camera.getZoom(),
				0, camera.getZoom(), 0, -camera.getY() * camera.getZoom(),
				0, 0, 0, 0,
				0, 0, 0, 1,
		};
	}
	
	public static float[] getAspectMatrix(float aspectRatio) {
		return new float[] {
				aspectRatio, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 0, 0,
				0, 0, 0, 1,
		};
	}
}
