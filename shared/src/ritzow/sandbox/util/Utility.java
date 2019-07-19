package ritzow.sandbox.util;

import static ritzow.sandbox.network.Protocol.BLOCK_INTERACT_COOLDOWN_NANOSECONDS;
import static ritzow.sandbox.network.Protocol.BLOCK_INTERACT_RANGE;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.SplittableRandom;
import java.util.function.BooleanSupplier;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

/** 
	Contains a number of static utility methods relating to various systems 
	(matrix/random math, time, synchronization, hitboxes) 
**/
public final class Utility {
	private Utility() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated");
	}

	public static String formatCurrentTime() {
		return "[" + LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) + "]";
	}

	public static boolean resendIntervalElapsed(long startTime, int attempts) {
		return System.nanoTime() >= Math.fma(Utility.millisToNanos(Protocol.RESEND_INTERVAL), attempts, startTime);
	}

	public static int splitQuantity(int bucketSize, int itemCount) {
		return itemCount/bucketSize + itemCount%bucketSize;
	}

	public static String formatAddress(InetSocketAddress address) {
		return "[" + address.getAddress().getHostAddress() + "]:" + 
				(address.getPort() == 0 ? "any" : address.getPort());
	}

	public static ProtocolFamily getProtocolFamily(InetAddress address) {
		if(address instanceof Inet6Address) {
			return StandardProtocolFamily.INET6;
		} else if(address instanceof Inet4Address) {
			return StandardProtocolFamily.INET;
		} else {
			throw new IllegalArgumentException("InetAddress of unknown protocol");
		}
	}

	public static int clampLowerBound(int min, float value) {
		return Math.max(min, (int)Math.floor(value));
	}

	public static int clampUpperBound(int max, float value) {
		return Math.min(max, (int)Math.ceil(value));
	}

	public interface GridAction {
		void perform(int x, int y);
	}

	public static void forEachBlock(BlockGrid grid, float leftX, float rightX, 
			float bottomY, float topY, GridAction action) {
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
	public static long updateWorld(World world, long previousTime, long maxTimestep) {
		long start = System.nanoTime(); //get the current time
		long updateTimeRemaining = start - previousTime;
		if(updateTimeRemaining > Protocol.MAX_UPDATE_TIME_ALLOWED)
			throw new IllegalArgumentException(updateTimeRemaining + 
					" is greater than the max allowed world update time");

		//update the world with a timestep of at most maxTimestep until the world is up to date.
		while(updateTimeRemaining > 0) {
			long singleUpdateTime = Math.min(updateTimeRemaining, maxTimestep);
			world.update(singleUpdateTime);
			updateTimeRemaining -= singleUpdateTime;
		}

		return start;
	}
	
	public static boolean canThrow(long lastThrowTime) {
		return Utility.nanosSince(lastThrowTime) > Protocol.THROW_COOLDOWN_NANOSECONDS;
	}
	
	public static boolean canBreak(PlayerEntity player, long lastBreakTime, BlockGrid blocks, int blockX, int blockY) {
		return Utility.nanosSince(lastBreakTime) > BLOCK_INTERACT_COOLDOWN_NANOSECONDS && 
			blocks.isValid(blockX, blockY) &&	
			blocks.isBlock(blockX, blockY) &&
			Utility.withinDistance(player.getPositionX(), player.getPositionY(), blockX, blockY, BLOCK_INTERACT_RANGE);
	}
	
	public static boolean canPlace(PlayerEntity player, long lastPlaceTime, BlockGrid blocks, int blockX, int blockY) {
		return Utility.nanosSince(lastPlaceTime) > Protocol.BLOCK_INTERACT_COOLDOWN_NANOSECONDS &&
				inRange(player, blockX, blockY) && 
				blocks.isValid(blockX, blockY) && 
				!blocks.isBlock(blockX, blockY) &&
				blocks.isSolidBlockAdjacent(blockX, blockY);
	}
	
	public static boolean inRange(Entity player, int blockX, int blockY) {
		return Utility.withinDistance(player.getPositionX(), player.getPositionY(),
				blockX, blockY, Protocol.BLOCK_INTERACT_RANGE);
	}

	public static void notify(Object o) {
		synchronized(o) {
			o.notifyAll();
		}
	}

	/**
	 * Waits on {@code lock} and returns once {@code condition} returns {@code true}.
	 * @param lock the object to wait to be notified by.
	 * @param condition the condition to check.
	 */
	public static void waitOnCondition(Object lock, BooleanSupplier condition) throws InterruptedException {
		if(!condition.getAsBoolean()) {
			synchronized(lock) {
				lock.wait();
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

	/** Returns an rotation in radians that represents the change in angle after rotating around a point **/
	public static float rotateAround(float x, float y, float centerX, float centerY) {
		return (float)Math.atan2(x-centerX, y-centerY);
	}

	public static float rotateCoordX(float x, float y, float centerX, float centerY, float angle) {
		return (float)Math.fma((x - centerX), Math.cos(angle), -(y - centerY) * Math.sin(angle)) + x;
	}

	public static float rotateCoordY(float x, float y, float centerX, float centerY, float angle) {
		return Math.fma((y - centerY), (float)Math.cos(angle), (x - centerX)*(float)Math.sin(angle)) + y;
	}

	public static void printUsedMemory() {
		System.out.println(formatSize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
	}

	public static void printTimeSince(long nanoseconds) {
		System.out.println(formatTime(System.nanoTime() - nanoseconds));
	}
	
	public static void printFramerate(long nanoseconds) {
		System.out.println(1_000_000_000/(System.nanoTime() - nanoseconds) + " FPS");
	}

	public static String formatSize(long bytes) {
		String units; double value;
		if(bytes < 1000) {
			units = "bytes";
			value = bytes;
		} else if(bytes/1000 < 1000) {
			units = "KB";
			value = bytes/1000d;
		} else if(bytes/1_000_000 < 1000) {
			units = "MB";
			value = bytes/1_000_000d;
		} else {
			units = "GB";
			value = bytes/1_000_000_000d;
		}
		return formatNumber(value, 2) + " " + units;
	}

	public static String formatTime(long nanoseconds) {
		String units; double value;
		if(nanoseconds < 1000) {
			units = "ns";
			value = nanoseconds;
		} else if(nanoseconds/1000 < 1000) {
			units = "μs";
			value = nanoseconds/1000f;
		} else if(nanoseconds/1_000_000 < 1000) {
			units = "ms";
			value = nanoseconds/1_000_000f;
		} else {
			units = "s";
			value = nanoseconds/1_000_000_000f;
		}
		return formatNumber(value, 2) + " " + units;
	}

	public static String formatNumber(double value, int decimals) {
		long asInteger = Math.round(value);
		String number = Double.toString(value);
		return asInteger == value ? Long.toString(asInteger) : (decimals > 0 ? 
			number.substring(0, Math.min(number.length(),
			number.indexOf('.') + decimals)) : ("~" + asInteger));
	}

	public static double millisBetween(long startNanos, long endNanos) {
		return nanosToMillis(endNanos - startNanos);
	}
	
	public static float convertAccelerationSecondsNanos(float acceleration) {
		return acceleration / 1_000_000_000f / 1_000_000_000f;
	}
	
	public static float convertPerSecondToPerNano(float value) {
		return value / 1_000_000_000;
	}

	public static long millisToNanos(long milliseconds) {
		return milliseconds * 1_000_000;
	}

	public static double nanosToMillis(long nanoseconds) {
		return nanoseconds / 1_000_000D;
	}

	public static long nanosSince(long nanosStart) {
		return System.nanoTime() - nanosStart;
	}

	public static double millisSince(long nanosStart) {
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
	
	public static float random(double min, double max) {
		return (float)new SplittableRandom().nextDouble(min, max);
	}
	
	public static void launchAtAngle(Entity e, float angle, float velocity) {
		e.setVelocityX((float)Math.cos(angle) * velocity);
		e.setVelocityY((float)Math.sin(angle) * velocity);
	}
	
	public static void launchAtRandomRatio(Entity e, double minFraction, double maxFraction, float velocity) {
		launchAtAngle(e, random(minFraction * 2 * Math.PI, maxFraction * 2 * Math.PI), velocity);
	}
	
	public static float randomAngleRadians() {
		return Utility.random(0, Math.PI * 2);
	}

	public static boolean intersection(float rectangleX, float rectangleY, 
			float width, float height, float pointX, float pointY) {
		return (pointX <= Math.fma(0.5f, width, rectangleX) && pointX >= Math.fma(width, 0.5f, -rectangleX)) && 
				(pointY <= Math.fma(height, 0.5f, rectangleY) && pointY >= Math.fma(height, 0.5f, -rectangleY));
	}

	public static boolean intersection(float x, float y, float width, float height, 
			float x2, float y2, float width2, float height2) {
		return (Math.abs(x - x2) * 2 < (width + width2)) && (Math.abs(y - y2) * 2 < (height + height2));
	}

	public static float average(float val1, float val2) {
		return (val1 + val1) * 0.5f;
	}

	public static double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.fma((x1-x2), (x1-x2), (y1-y2)*(y1-y2)));
	}

	public static float distance(float x1, float y1, float x2, float y2) {
		return (float)Math.sqrt(distanceSquared(x1, y1, x2, y2));
	}

	public static boolean withinDistance(float x1, float y1, float x2, float y2, float distance) {
		return distanceSquared(x1, y1, x2, y2) <= distance*distance;
	}

	private static float distanceSquared(float x1, float y1, float x2, float y2) {
		return Math.fma((x1-x2), (x1-x2), (y1-y2)*(y1-y2));
	}

	/** Returns the maximum value of the component opposite the one passed in (x if y given) within a given radius **/
	public static float maxComponentInRadius(float otherComp, float radius) {
		return (float)Math.sqrt(Math.fma(radius, radius, -otherComp * otherComp));
	}
}
