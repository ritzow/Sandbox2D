package util;

public class TimeUtil {
	/**
	 * Get the time elapsed between two times
	 * @param initial the start time in nanoseconds
	 * @param end the end time in nanoseconds
	 * @return the elapsed time in milliseconds
	 */
	public static float getElapsedTime(long initial, long end) {
		return (float)((end - initial) * 0.000001);
	}
	
	public static float nanoToMillis(long nano) {
		return (float)(nano * 0.000001);
	}
}
