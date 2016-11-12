package util;

public class Utility {
	
	public static class MathUtility {
		public static float randomFloat(float min, float max) {
			return (float)(Math.random() * (max - min) + min);
		}
	}
	
	public static class Time {
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
	
	public static class Synchronizer {
		public static void waitForSetup(Installable installable) {
			synchronized(installable) {
				while(!installable.isSetupComplete()) {
					try {
						installable.wait();
					} catch (InterruptedException e){
						e.printStackTrace();
					}
				}
			}
		}
		
		public static void waitForExit(Exitable exitable) {
			exitable.exit();
			synchronized(exitable) {
				while(!exitable.isFinished()) {
					try {
						exitable.wait();
					} catch (InterruptedException e){
						e.printStackTrace();
					}
				}
			}
		}
	}

	
	public static final class Matrix {
		
		/** return the transpose of a 4 by 4 matrix **/
		public static final float[] getTranspose(float[] matrix) {
			return new float[] {
					matrix[0], matrix[4], matrix[8], matrix[12],
					matrix[1], matrix[5], matrix[9], matrix[13],
					matrix[2], matrix[6], matrix[10], matrix[14],
					matrix[3], matrix[7], matrix[11], matrix[15],
			};
		}
		
		public static float[] multiply(float[] a, float[] b) {
			float[] result = new float[16];
			for(int i = 0; i < 4; i++){
				for(int j = 0; j < 4; j++){
					for(int k = 0; k < 4; k++){
						set(result, i, j, get(result, i, j) + get(a, i, k) * get(b, k, j));
					}
				}
			}
			return result;
		}
		
		public static void multiply(float[] a, float[] b, float[] destination) {
			for(int i = 0; i < 4; i++){
				for(int j = 0; j < 4; j++){
					for(int k = 0; k < 4; k++){
						set(destination, i, j, get(destination, i, j) + get(a, i, k) * get(b, k, j));
					}
				}
			}
		}
		
		private static float get(float[] array, int row, int column) {
			return array[(row * 4) + column];
		}
		
		private static void set(float[] array, int row, int column, float value) {
			array[(row * 4) + column] =  value;
		}
	}
	
	public static class Intersection {
		public static boolean intersection(float rectangleX, float rectangleY, float width, float height, float pointX, float pointY) {
			return (pointX <= rectangleX + width/2 && pointX >= rectangleX - width/2) && (pointY <= rectangleY + height/2 && pointY >= rectangleY - height/2);
		}
		
		public static boolean intersection(float x, float y, float width, float height, float x2, float y2, float width2, float height2) {
			return (Math.abs(x - x2) * 2 < (width + width2)) && (Math.abs(y - y2) * 2 < (height + height2));
		}
		
		public static float combineFriction(float friction1, float friction2) {
			return (friction1 + friction2)/2;
		}
	}

}
