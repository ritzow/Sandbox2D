package ritzow.sandbox.util;

import java.util.function.BooleanSupplier;

/**
 * Contains a number of static utility methods relating to various systems (matrix/random math, time, synchronization, hitboxes)
 * @author Solomon Ritzow
 *
 */
public final class Utility {
	
	private Utility() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated");
	}
	
	/**
	 * Waits on {@code lock} and returns once {@code condition} returns {@code true}.
	 * @param lock the object to wait to be notified by.
	 * @param object the condition to check.
	 */
	public static void waitOnCondition(Object lock, BooleanSupplier condition) {
		if(!condition.getAsBoolean()) {
			synchronized(lock) {
				while(!condition.getAsBoolean()) {
					try {
						lock.wait();
					} catch(InterruptedException e) {
						throw new RuntimeException("waitOnCondition should not be interrupted", e);
					}
				}
			}
		}
	}
	
	public static float millisToTime(long millis) {
		return millis * 0.0625f; //TODO not sure if this is correct
	}
	
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
