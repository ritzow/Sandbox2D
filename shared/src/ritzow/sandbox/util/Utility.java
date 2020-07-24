package ritzow.sandbox.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.SplittableRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntFunction;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

import static ritzow.sandbox.network.Protocol.BLOCK_INTERACT_COOLDOWN_NANOSECONDS;
import static ritzow.sandbox.network.Protocol.BLOCK_INTERACT_RANGE;

/**
	Contains a number of static utility methods relating to various systems
	(matrix/random math, time, synchronization, hitboxes)
**/
public final class Utility {

	private static final boolean USE_LOCK_SUPPORT = false;

	private Utility() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated");
	}

	public static void limitFramerate(long frameStart, long frameTimeLimit) {
		//frameTimeLimit - (System.nanoTime() - frameStart)
		long nanos = frameTimeLimit + frameStart - System.nanoTime();
		if(USE_LOCK_SUPPORT) {
			//TODO causing problems again, works when I run VisualVM (maybe has to do with high resolution timer configuration setting?)
			LockSupport.parkNanos(nanos);
		} else if(nanos > 0) {
			try {
				Thread.sleep(nanosToMillis(nanos));
			} catch(InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static long frameRateToFrameTimeNanos(long fps) {
		return 1_000_000_000/fps;
	}

	public static String formatCurrentTime() {
		return "[" + LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)) + "]";
	}

	public static boolean resendIntervalElapsed(long startTime, int attempts) {
		return System.nanoTime() >= Utility.millisToNanos(Protocol.RESEND_INTERVAL) * attempts + startTime;
	}

	public static int splitQuantity(int bucketSize, int itemCount) {
		return itemCount/bucketSize + itemCount%bucketSize;
	}

	public static float clamp(float min, float value, float max) {
		float first = Math.min(value, max);
		return Math.max(min, first);
	}

	public static ByteBuffer loadCompressedFile(Path file) throws IOException {
		return Bytes.decompress(load(file, ByteBuffer::allocate));
	}

	public static ByteBuffer load(Path file, IntFunction<ByteBuffer> buffer) throws IOException {
		try(FileChannel reader = FileChannel.open(file, StandardOpenOption.READ)) {
			long length = reader.size();
			if(length > Integer.MAX_VALUE)
				throw new RuntimeException(file + " too large " + formatSize(length));
			ByteBuffer dest = buffer.apply((int)length);
			reader.read(dest);
			return dest.flip();
		}
	}

	/**
	 * Updates the world
	 * @param world the world to update
	 * @param previousTime the previous update start time
	 * @param maxTimestep the maximum amount of game time to update the world at once
	 * @return the start time for the world update (should replace the previous time)
	 */
	public static long updateWorld(World world, long previousTime, long maxTimestep) {
		long start = System.nanoTime(); //get the current time
		long timeRemaining = start - previousTime;
		if(timeRemaining > Protocol.MAX_UPDATE_TIME_ALLOWED)
			throw new IllegalArgumentException(timeRemaining +
					" is greater than the max allowed world update time");
		//update the world with a timestep of at most maxTimestep until the world is up to date.
		while(timeRemaining > 0) {
			long singleUpdateTime = Math.min(timeRemaining, maxTimestep);
			world.update(singleUpdateTime);
			timeRemaining -= singleUpdateTime;
		}
		return start;
	}

	public static boolean canThrow(long lastThrowTime) {
		return Utility.nanosSince(lastThrowTime) > Protocol.THROW_COOLDOWN_NANOSECONDS;
	}

	public static boolean canBreak(PlayerEntity player, long lastBreakTime, World world, int blockX, int blockY) {
		return Utility.nanosSince(lastBreakTime) > BLOCK_INTERACT_COOLDOWN_NANOSECONDS &&
			world.getBlocks().isValidAndBlock(World.LAYER_MAIN, blockX, blockY) &&
			Utility.withinDistance(player.getPositionX(), player.getPositionY(), blockX, blockY, BLOCK_INTERACT_RANGE);
	}

	public static boolean canPlace(PlayerEntity player, long lastPlaceTime, World world, int blockX, int blockY) {
		BlockGrid blocks = world.getBlocks();
		return Utility.nanosSince(lastPlaceTime) > BLOCK_INTERACT_COOLDOWN_NANOSECONDS &&
			inRange(player, blockX, blockY) &&
			blocks.isValid(World.LAYER_MAIN, blockX, blockY) && !blocks.isBlock(World.LAYER_MAIN, blockX, blockY) &&
			(blocks.isSolidBlockAdjacent(World.LAYER_MAIN, blockX, blockY) || blocks.isSolidBlockAdjacent(World.LAYER_BACKGROUND, blockX, blockY));
	}

	public static boolean inRange(Entity player, int blockX, int blockY) {
		return Utility.withinDistance(player.getPositionX(), player.getPositionY(),
				blockX, blockY, BLOCK_INTERACT_RANGE);
	}

	public static double degreesPerSecToRadiansPerNano(double degreesPerSec) {
		return degreesPerSec / 1_000_000_000 * Math.PI / 180;
	}

	/** Put an angle, in radians, from 0 to 2 PI */
	public static float normalizeAngle(double angle) {
		return (float)(angle - 2*Math.PI * Math.floor(angle/(2*Math.PI)));
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
		long frameTime = System.nanoTime() - nanoseconds;
		System.out.println(1_000_000_000/frameTime + " FPS (" + Utility.formatTime(frameTime) + ")");
	}

	public static String formatSize(long bytes) {
		if(bytes < 1000) {
			return formatNumber(bytes, 0) + " bytes";
		} else if(bytes/1000 < 1000) {
			return formatNumber(bytes/1000d, 2) + " KB";
		} else if(bytes/1_000_000 < 1000) {
			return formatNumber(bytes/1_000_000d, 2) + " MB";
		} else {
			return formatNumber(bytes/1_000_000_000d, 2) + " GB";
		}
	}

	public static String formatTime(long nanoseconds) {
		if(nanoseconds < 1000) {
			return formatNumber(nanoseconds, 0) + " ns";
		} else if(nanoseconds/1000 < 1000) {
			return formatNumber(nanoseconds/1000d, 2) + " μs";
		} else if(nanoseconds/1_000_000 < 1000) {
			return formatNumber(nanoseconds/1_000_000d, 2) + " ms";
		} else {
			return formatNumber(nanoseconds/1_000_000_000d, 2) + " s";
		}
	}

	//TODO fix formatNumber to not have edge cases and be faster
	public static String formatNumber(double value, int decimals) {
		long asInteger = Math.round(value);
		//BigDecimal dec = new BigDecimal(value, new MathContext(decimals));
		String number = Double.toString(value);
		return asInteger == value ? Long.toString(asInteger) : (decimals > 0 ?
			number.substring(0, Math.min(number.length(),
			number.indexOf('.') + decimals)) : ("~" + asInteger));
	}

	public static double millisBetween(long startNanos, long endNanos) {
		return nanosToMillis(endNanos - startNanos);
	}

	public static float convertAccelerationSecondsNanos(float acceleration) {
		return acceleration * 1.0E-18f;
	}

	public static float convertPerSecondToPerNano(float value) {
		return value * 1.0E-09f;
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
			return magnitude;
	}

	private static final SplittableRandom RANDOM = new SplittableRandom();

	public static float random(double min, double max) {
		return (float)RANDOM.nextDouble(min, max);
	}

	public static float oscillate(double radiansPerNano) {
		return oscillate(radiansPerNano, 0, 0, 1);
	}

	public static float oscillate(double radiansPerNano, float offsetRadians, float min, float max) {
		return oscillate(System.nanoTime(), radiansPerNano, offsetRadians, min, max);
	}

	public static float oscillate(long time, double radiansPerNano, float offsetRadians, float min, float max) {
		return convertRange(-1.0f, 1.0f, min, max, (float)Math.cos(Utility.normalizeAngle(time * radiansPerNano + offsetRadians)));
	}

	public static void launchAtAngle(Entity e, float angle, float velocity) {
		e.setVelocityX((float)Math.cos(angle) * velocity);
		e.setVelocityY((float)Math.sin(angle) * velocity);
	}

	public static void launchAtRandomRatio(Entity e, double minFraction, double maxFraction, float velocity) {
		launchAtAngle(e, random(minFraction * 2 * Math.PI, maxFraction * 2 * Math.PI), velocity);
	}

	public static float randomAngleRadians() {
		return random(0, Math.PI * 2);
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

	/** Returns the maximum value of the component opposite the one passed in (x if y given) within a given radius
	 * @param otherComp x or y component
	 * @param radius radius from given component
	 * @return the other component **/
	public static float maxComponentInRadius(float otherComp, float radius) {
		return (float)Math.sqrt(Math.fma(radius, radius, -otherComp * otherComp));
	}
}
