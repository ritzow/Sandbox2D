package ritzow.sandbox.util;

/**
 * Contains a number of static utility methods relating to various systems (matrix/random math, time, synchronization, hitboxes)
 * @author Solomon Ritzow
 *
 */
public final class Utility {
	
	private Utility() {}
	
	public static float randomFloat(float min, float max) {
		return (float)(Math.random() * (max - min) + min);
	}
	
	public static long randomLong(long min, long max) {
		return (long)(Math.random() * (max - min) + min);
	}
	
	public static boolean intersection(float rectangleX, float rectangleY, float width, float height, float pointX, float pointY) {
		return (pointX <= rectangleX + width/2 && pointX >= rectangleX - width/2) && (pointY <= rectangleY + height/2 && pointY >= rectangleY - height/2);
	}
	
	public static boolean intersection(float x, float y, float width, float height, float x2, float y2, float width2, float height2) {
		return (Math.abs(x - x2) * 2 < (width + width2)) && (Math.abs(y - y2) * 2 < (height + height2));
	}
	
	public static float combineFriction(float friction1, float friction2) {
		return (friction1 + friction2)/2;
	}
	
	public static double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
	}
}
