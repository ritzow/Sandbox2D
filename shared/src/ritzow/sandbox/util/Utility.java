package ritzow.sandbox.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.Deserializer;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;

/**
 * Contains a number of static utility methods relating to various systems (matrix/random math, time, synchronization, hitboxes)
 * @author Solomon Ritzow
 *
 */
public final class Utility {
	private Utility() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated");
	}
	
	public static <T extends InetAddress> T getPublicAddress(Class<T> addressType) throws SocketException {
		Set<InetAddress> addresses = NetworkInterface.networkInterfaces()
			.filter(network -> {
				try {
					return network.isUp() && !network.isLoopback();
				} catch (SocketException e) {
					e.printStackTrace();
					return false;
				}
			})
			.map(network -> network.inetAddresses())
			.reduce((a, b) -> Stream.concat(a, b))
			.get().filter(address -> addressType.isInstance(address))
			.collect(Collectors.toSet());
		if(addresses.isEmpty())
			throw new RuntimeException("No address of type " + addressType + " available");
		return addressType.cast(addresses.iterator().next());
	}

	public static World loadWorld(File file, Deserializer des) {
		try(FileInputStream in = new FileInputStream(file)) {
			byte[] data = new byte[(int)file.length()];
			in.read(data);
			World world = des.deserialize(ByteUtil.decompress(data));
			return world;
		} catch(IOException e) {
			throw new RuntimeException("Error loading world from file ", e);
		}
	}
	
	public interface GridAction {
		void perform(int x, int y);
	}
	
	public static int clampLowerBound(int min, float value) {
		return Math.max(min, (int)Math.floor(value));
	}
	
	public static int clampUpperBound(int max, float value) {
		return Math.min(max, (int)Math.ceil(value));
	}
	
	public static void performAction(BlockGrid grid, float leftX, float rightX, float bottomY, float topY, GridAction action) {
		//calculate block grid bounds TODO fix after adding chunk system, allow for negatives
		int leftBound = 	clampLowerBound(0, leftX);
		int rightBound = 	clampUpperBound(grid.getWidth()-1, rightX);
		int topBound = 		clampUpperBound(grid.getHeight()-1, topY);
		int bottomBound = 	clampLowerBound(0, bottomY);
		
		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				action.perform(column, row);
			}
		}
	}
	
	/**
	 * Sleeps for the provided number of milliseconds
	 * @param milliseconds the number of milliseconds to sleep for
	 * @throws IllegalStateException if the thread is interrupted while sleeping
	 */
	public static void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch(InterruptedException e) {
			throwInterrupted(e);
		}
	}
	
	private static void throwInterrupted(InterruptedException cause) {
		throw new IllegalStateException(Thread.currentThread() + " should not be interrupted", cause);
	}
	
	/**
	 * Updates the world
	 * @param world the world to update
	 * @param previousTime the previous update start time
	 * @param maxTimestep the maximum amount of game time to update the world at once
	 * @param timeScale the time conversion (from nanoseconds to game time)
	 * @return the start time for the world update (should replace the previous time)
	 */
	public static long updateWorld(World world, long previousTime, float maxTimestep, float timeScale) {
		if(previousTime == 0)
			throw new IllegalArgumentException("previousTime is 0");
		long current = System.nanoTime(); //get the current time
		float totalUpdateTime = (current - previousTime) / timeScale;
		
		//update the world with a timestep of at most maxTimestep until the world is up to date.
		for(float time = totalUpdateTime; totalUpdateTime > 0; totalUpdateTime -= time) {
			time = Math.min(totalUpdateTime, maxTimestep);
			world.update(time);
			totalUpdateTime -= time;
		}
		return current;
	}
	
	public static void notify(Object o) {
		synchronized(o) {
			o.notifyAll();
		}
	}
	
	public static void waitUnconditionally(Object o) {
		try {
			synchronized(o) {
				o.wait();
			}
		} catch (InterruptedException e) {
			throwInterrupted(e);
		}
	}
	
	/**
	 * Waits on {@code lock} and returns once {@code condition} returns {@code true}.
	 * @param lock the object to wait to be notified by.
	 * @param condition the condition to check.
	 */
	public static void waitOnCondition(Object lock, BooleanSupplier condition) {
		if(!condition.getAsBoolean()) {
			synchronized(lock) {
				try {
					lock.wait();
				} catch(InterruptedException e) {
					throwInterrupted(e);
				}
				if(!condition.getAsBoolean())
					throw new IllegalStateException("condition not met");
			}
		}
	}
	
	public static void waitOnCondition(Object lock, long timeoutMillis, BooleanSupplier condition) {
		if(timeoutMillis == 0)
			throw new IllegalArgumentException("timeout of 0 not allowed, use other overload");
		if(!condition.getAsBoolean()) {
			long start = System.currentTimeMillis();
			synchronized(lock) {
				try {
					lock.wait(timeoutMillis);
				} catch(InterruptedException e) {
					throwInterrupted(e);
				}
			}
			if((System.currentTimeMillis() - start) < timeoutMillis && !condition.getAsBoolean())
				throw new IllegalStateException("condition not met");
		}
	}
	
	public static float convertRange(float oldMin, float oldMax, float newMin, float newMax, float value) {
		return (((value - oldMin) * (newMax - newMin)) / (oldMax - oldMin)) + newMin;
	}
	
	//TODO implement rotateAround
	public static float rotateAround(float radians, float radius, float centerX, float centerY) {
		throw new UnsupportedOperationException("not implemented");
	}
	
	public static long millisBetween(long startNanos, long endNanos) {
		return nanosToMillis(endNanos - startNanos);
	}
	
	public static long millisToNanos(long milliseconds) {
		return milliseconds * 1_000_000;
	}
	
	public static long nanosToMillis(long nanoseconds) {
		return nanoseconds / 1_000_000;
	}
	
	public static long nanosSince(long nanosStart) {
		return System.nanoTime() - nanosStart;
	}
	
	public static long millisSince(long nanosStart) {
		return nanosToMillis(nanosSince(nanosStart));
	}
	
	public static float addMagnitude(float number, float magnitude) {
		if(number < 0)
			return Math.min(0, number - magnitude);
		else if(number > 0)
			return Math.max(0, number + magnitude);
		else
			return 0;
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
	
	public static float average(float val1, float val2) {
		return (val1 + val1)/2;
	}
	
	public static double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
	}
	
	public static float distance(float x1, float y1, float x2, float y2) {
		return (float)Math.sqrt(distanceSquared(x1, y1, x2, y2));
	}
	
	public static boolean withinDistance(float x1, float y1, float x2, float y2, float distance) {
		return distanceSquared(x1, y1, x2, y2) <= distance*distance;
	}
	
	private static float distanceSquared(float x1, float y1, float x2, float y2) {
		return (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2);
	}
}
